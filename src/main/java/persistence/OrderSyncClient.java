package persistence;

import ui.OrderQueuePanel;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * WebSocket client — one instance per panel (OrderingPanel / OrderQueuePanel).
 *
 * Responsibilities
 * ────────────────
 * • Connects to {@link OrderSyncServer} on
 * localhost:{@link OrderSyncServer#PORT}.
 * • Reconnects automatically if the connection drops (backoff up to 30 s).
 * • Sends NEW_ORDER and STATUS_CHANGE payloads to the server.
 * • Delivers inbound messages to the registered callbacks on the EDT.
 *
 * Usage
 * ─────
 * 
 * <pre>
 * OrderSyncClient client = new OrderSyncClient();
 *
 * // Called when another instance places a new order
 * client.setOnNewOrder(receipt -> SwingUtilities.invokeLater(() -> orderQueuePanel.addOrder(receipt)));
 *
 * // Called when another instance changes an order's kitchen status
 * client.setOnStatusChange((orderId, statusCode) -> SwingUtilities
 *         .invokeLater(() -> orderQueuePanel.applyRemoteStatus(orderId, statusCode)));
 *
 * client.connect();
 * </pre>
 */
public class OrderSyncClient {

    // ── Callbacks registered by the owning panel ──────────────────────────

    /** Fired (on EDT) when the server pushes a new order from another instance. */
    private Consumer<OrderQueuePanel.Receipt> onNewOrder;

    /**
     * Fired (on EDT) when the server pushes a status change.
     * int[0] = orderId, int[1] = statusCode
     */
    private Consumer<int[]> onStatusChange;
    private Consumer<String> onMenuUpdate;
    private Consumer<String> onInventoryUpdate;

    // ── Internal state ────────────────────────────────────────────────────

    private volatile boolean connected = false;
    private volatile boolean shutdown = false;
    private volatile OutputStream outStream = null;
    private volatile Socket socket = null;

    private static final int MAX_BACKOFF_MS = 30_000;
    private static final int INITIAL_BACKOFF_MS = 500; // Reduced from 1s to 500ms for faster recovery

    // ── Public API ─────────────────────────────────────────────────────────

    public void setOnNewOrder(Consumer<OrderQueuePanel.Receipt> callback) {
        this.onNewOrder = callback;
    }

    public void setOnStatusChange(Consumer<int[]> callback) {
        this.onStatusChange = callback;
    }

    public void setOnMenuUpdate(Consumer<String> callback) {
        this.onMenuUpdate = callback;
    }

    public void setOnInventoryUpdate(Consumer<String> callback) {
        this.onInventoryUpdate = callback;
    }

    /**
     * Start the connect + read loop in a daemon thread.
     * Returns immediately; connection happens in background.
     */
    public void connect() {
        Thread t = new Thread(this::connectLoop, "OrderSyncClient");
        t.setDaemon(true);
        t.start();
    }

    /** Gracefully disconnect and stop reconnect attempts. */
    public void disconnect() {
        shutdown = true;
        closeSocket();
    }

    /**
     * Send a NEW_ORDER payload to the server (which stores it in SQLite
     * and broadcasts it to all other connected clients).
     */
    public void publishNewOrder(OrderQueuePanel.Receipt receipt) {
        sendJson(OrderPayload.fromReceipt(receipt).toJson());
    }

    /**
     * Send a STATUS_CHANGE payload to the server.
     *
     * @param orderId    the POS order ID
     * @param statusCode one of OrderQueuePanel.STATUS_* constants
     */
    public void publishStatusChange(int orderId, int statusCode) {
        sendJson(OrderPayload.statusChange(orderId, statusCode).toJson());
    }

    public void publishMenuUpdate(String entityJson) {
        sendJson(OrderPayload.menuUpdate(entityJson).toJson());
    }

    public void publishInventoryUpdate(String entityJson) {
        sendJson(OrderPayload.inventoryUpdate(entityJson).toJson());
    }

    // ── Connection loop ───────────────────────────────────────────────────

    private void connectLoop() {
        int backoff = INITIAL_BACKOFF_MS;

        while (!shutdown) {
            try {
                attemptConnect();
                backoff = INITIAL_BACKOFF_MS; // reset on success
            } catch (Exception e) {
                connected = false;
                System.out.println("[OrderSyncClient] Disconnected — retry in " + (backoff / 1000) + "s");
                sleep(backoff);
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            }
        }
    }

    /**
     * Open a socket, perform the WebSocket handshake, then block reading
     * frames until the connection closes.
     */
    private void attemptConnect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", OrderSyncServer.PORT), 5_000);
        socket.setSoTimeout(0); // block indefinitely on reads

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        this.outStream = out;

        // ── HTTP → WebSocket upgrade ──────────────────────────────────────
        String key = generateWebSocketKey();
        String request = "GET /ws/orders HTTP/1.1\r\n" +
                "Host: 127.0.0.1:" + OrderSyncServer.PORT + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: " + key + "\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // Read until blank line (end of HTTP response headers)
        readHttpResponse(in);

        connected = true;
        System.out.println("[OrderSyncClient] Connected to OrderSyncServer");

        // ── Frame read loop ───────────────────────────────────────────────
        while (!socket.isClosed() && !shutdown) {
            String msg = readFrame(in);
            if (msg == null)
                break; // server closed connection
            dispatch(msg);
        }
    }

    /** Parse an inbound JSON payload and fire the appropriate callback. */
    private void dispatch(String json) {
        try {
            OrderPayload p = OrderPayload.fromJson(json);

            if (OrderPayload.TYPE_NEW_ORDER.equals(p.type) && onNewOrder != null) {
                OrderQueuePanel.Receipt receipt = p.toReceipt();
                javax.swing.SwingUtilities.invokeLater(() -> onNewOrder.accept(receipt));

            } else if (OrderPayload.TYPE_STATUS_CHANGE.equals(p.type) && onStatusChange != null) {
                int[] payload = { p.orderId, p.statusCode };
                javax.swing.SwingUtilities.invokeLater(() -> onStatusChange.accept(payload));
            } else if (OrderPayload.TYPE_MENU_UPDATE.equals(p.type) && onMenuUpdate != null) {
                final String ent = p.entityJson == null ? "" : p.entityJson;
                javax.swing.SwingUtilities.invokeLater(() -> onMenuUpdate.accept(ent));
            } else if (OrderPayload.TYPE_INVENTORY_UPDATE.equals(p.type) && onInventoryUpdate != null) {
                final String ent = p.entityJson == null ? "" : p.entityJson;
                javax.swing.SwingUtilities.invokeLater(() -> onInventoryUpdate.accept(ent));
            }
        } catch (Exception e) {
            System.err.println("[OrderSyncClient] dispatch error: " + e.getMessage());
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────

    private synchronized void sendJson(String json) {
        if (!connected || outStream == null)
            return;
        try {
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            // Client frames MUST be masked (RFC 6455 §5.3)
            byte[] mask = new byte[4];
            new SecureRandom().nextBytes(mask);

            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(0x81); // FIN + TEXT opcode

            if (payload.length <= 125) {
                frame.write(0x80 | payload.length); // MASK bit set
            } else if (payload.length <= 65535) {
                frame.write(0x80 | 126);
                frame.write((payload.length >> 8) & 0xFF);
                frame.write(payload.length & 0xFF);
            } else {
                frame.write(0x80 | 127);
                long len = payload.length;
                for (int i = 7; i >= 0; i--) {
                    frame.write((int) ((len >> (i * 8)) & 0xFF));
                }
            }

            frame.write(mask);

            byte[] masked = new byte[payload.length];
            for (int i = 0; i < payload.length; i++) {
                masked[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
            frame.write(masked);

            outStream.write(frame.toByteArray());
            outStream.flush();

        } catch (IOException e) {
            connected = false;
            closeSocket();
        }
    }

    // ── WebSocket frame reader (server frames are NOT masked) ─────────────

    private String readFrame(InputStream in) throws IOException {
        int b0 = in.read();
        if (b0 < 0)
            return null;

        int opcode = b0 & 0x0F;
        if (opcode == 0x8)
            return null; // CLOSE

        int b1 = in.read();
        if (b1 < 0)
            return null;

        long payloadLen = b1 & 0x7F;
        if (payloadLen == 126) {
            payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            payloadLen = 0;
            for (int i = 0; i < 8; i++) {
                payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
            }
        }

        byte[] payload = new byte[(int) payloadLen];
        int read = 0;
        while (read < payload.length) {
            int r = in.read(payload, read, payload.length - read);
            if (r < 0)
                return null;
            read += r;
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    // ── HTTP handshake helpers ────────────────────────────────────────────

    private void readHttpResponse(InputStream in) throws IOException {
        // Read header lines until we hit the blank line
        int prev = -1, curr;
        int crlfCount = 0;
        while ((curr = in.read()) >= 0) {
            if (prev == '\r' && curr == '\n')
                crlfCount++;
            else if (curr != '\r')
                crlfCount = 0;
            if (crlfCount == 2)
                break; // \r\n\r\n
            prev = curr;
        }
    }

    private String generateWebSocketKey() {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException ignored) {
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
