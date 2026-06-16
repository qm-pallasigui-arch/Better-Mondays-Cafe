package persistence;

import persistence.sqlite.SQLiteOrderRepository;
import ui.OrderQueuePanel;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Base64;

/**
 * Embedded WebSocket server — pure Java, zero external dependencies.
 *
 * How it works
 * ────────────
 * 1. Starts a ServerSocket on {@link #PORT} (default 8765).
 * 2. One instance runs per machine (first caller wins via
 * {@link #startIfNotRunning()}).
 * 3. Every connected client (OrderSyncClient) can send a JSON payload.
 * 4. The server saves the change to SQLite via SQLiteOrderRepository,
 * then broadcasts the same payload to every OTHER connected client.
 *
 * This means every Kitchen / POS window on the same machine (or LAN)
 * stays in sync in real time.
 *
 * Usage (call once at application startup):
 * 
 * <pre>
 * OrderSyncServer.startIfNotRunning();
 * </pre>
 */
public class OrderSyncServer {

    public static final int PORT = 8765;

    private static final AtomicBoolean running = new AtomicBoolean(false);

    // All currently connected WebSocket client sockets
    private static final Set<ClientHandler> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final SQLiteOrderRepository repo = new SQLiteOrderRepository();

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Start the server in a daemon thread if it isn't already running.
     * Safe to call multiple times — only the first call has any effect.
     */
    public static void startIfNotRunning() {
        if (!running.compareAndSet(false, true))
            return;

        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                serverSocket.setReuseAddress(true);
                System.out.println("[OrderSyncServer] Listening on port " + PORT);
                while (true) {
                    Socket client = serverSocket.accept();
                    // Hand off to a per-client thread
                    Thread ct = new Thread(new ClientHandler(client));
                    ct.setDaemon(true);
                    ct.start();
                }
            } catch (IOException e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                // Bind failures often mean another instance already bound the port.
                if (e instanceof java.net.BindException) {
                    // Probe localhost:PORT to see if a server is responding — if so, treat as
                    // already running.
                    try (Socket probe = new Socket()) {
                        probe.connect(new InetSocketAddress("127.0.0.1", PORT), 1000);
                        System.out.println("[OrderSyncServer] Port " + PORT
                                + " already bound; another server responsive — assuming running.");
                        running.set(true);
                        return;
                    } catch (IOException connEx) {
                        System.err.println("[OrderSyncServer] Port " + PORT + " is in use but not responding to probe: "
                                + connEx.getMessage());
                        running.set(false);
                        return;
                    }
                }
                System.err.println("[OrderSyncServer] Server error: " + msg);
                running.set(false);
            }
        }, "OrderSyncServer");
        t.setDaemon(true);
        t.start();
    }

    /** Broadcast a raw JSON string to all connected clients except the sender. */
    static void broadcast(String json, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.sendText(json);
            }
        }
    }

    /** Broadcast to ALL clients including sender (used for DB-sourced reload). */
    static void broadcastAll(String json) {
        for (ClientHandler c : clients) {
            c.sendText(json);
        }
    }

    // ── ClientHandler — one per connected WebSocket ───────────────────────

    static class ClientHandler implements Runnable {

        private final Socket socket;
        private OutputStream out;
        private boolean handshakeDone = false;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();

                // ── 1. WebSocket handshake ──────────────────────────────
                if (!doHandshake(in, out)) {
                    socket.close();
                    return;
                }
                handshakeDone = true;
                clients.add(this);
                System.out.println("[OrderSyncServer] Client connected. Total: " + clients.size());

                // ── 2. Send active orders to the newly connected client ─
                sendActiveOrdersToClient();

                // ── 3. Message read loop ────────────────────────────────
                while (!socket.isClosed()) {
                    String msg = readFrame(in);
                    if (msg == null)
                        break; // connection closed
                    handleMessage(msg);
                }

            } catch (IOException e) {
                // Client disconnected — normal
            } finally {
                clients.remove(this);
                System.out.println("[OrderSyncServer] Client disconnected. Total: " + clients.size());
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        /** Process one incoming JSON payload from a client. */
        private void handleMessage(String json) {
            try {
                OrderPayload p = OrderPayload.fromJson(json);

                if (OrderPayload.TYPE_NEW_ORDER.equals(p.type)) {
                    // Persist to SQLite
                    repo.saveOrder(p.toReceipt());
                    // Broadcast to everyone else
                    broadcast(json, this);

                } else if (OrderPayload.TYPE_STATUS_CHANGE.equals(p.type)) {
                    // Persist status update
                    repo.updateOrderStatus(p.orderId, p.statusCode);
                    // Broadcast to everyone else
                    broadcast(json, this);
                } else if (OrderPayload.TYPE_MENU_UPDATE.equals(p.type)) {
                    // Menu updates are broadcast to other clients to trigger reloads
                    broadcast(json, this);
                } else if (OrderPayload.TYPE_INVENTORY_UPDATE.equals(p.type)) {
                    // Inventory updates are broadcast to other clients to trigger reloads
                    broadcast(json, this);
                }
            } catch (Exception e) {
                System.err.println("[OrderSyncServer] handleMessage error: " + e.getMessage());
            }
        }

        /**
         * Push all currently active orders (from SQLite) to this
         * newly connected client so it immediately sees the live queue.
         */
        private void sendActiveOrdersToClient() {
            try {
                List<OrderQueuePanel.Receipt> active = repo.loadActiveOrders();
                for (OrderQueuePanel.Receipt r : active) {
                    // Also send the current status so the client puts the
                    // card in the right lane
                    int statusCode = repo.getOrderStatus(r.orderId);

                    // Always send the NEW_ORDER payload first (creates the card)
                    String newOrderJson = OrderPayload.fromReceipt(r).toJson();
                    sendText(newOrderJson);

                    // If it has progressed beyond PENDING, send the status update too
                    if (statusCode > SQLiteOrderRepository.STATUS_PENDING) {
                        String statusJson = OrderPayload.statusChange(r.orderId, statusCode).toJson();
                        sendText(statusJson);
                    }
                }
            } catch (Exception e) {
                System.err.println("[OrderSyncServer] sendActiveOrdersToClient error: " + e.getMessage());
            }
        }

        // ── WebSocket frame codec (RFC 6455) ──────────────────────────────

        /** Read one text frame from the WebSocket stream. Returns null on close. */
        private String readFrame(InputStream in) throws IOException {
            // Byte 0: FIN + opcode
            int b0 = in.read();
            if (b0 < 0)
                return null;

            int opcode = b0 & 0x0F;
            if (opcode == 0x8)
                return null; // CLOSE frame

            // Byte 1: MASK bit + payload length
            int b1 = in.read();
            if (b1 < 0)
                return null;
            boolean masked = (b1 & 0x80) != 0;
            long payloadLen = b1 & 0x7F;

            if (payloadLen == 126) {
                payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else if (payloadLen == 127) {
                payloadLen = 0;
                for (int i = 0; i < 8; i++) {
                    payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
                }
            }

            // Masking key (4 bytes) — client frames are always masked
            byte[] maskKey = new byte[4];
            if (masked) {
                int read = 0;
                while (read < 4) {
                    int r = in.read(maskKey, read, 4 - read);
                    if (r < 0)
                        return null;
                    read += r;
                }
            }

            // Payload
            byte[] payload = new byte[(int) payloadLen];
            int read = 0;
            while (read < payload.length) {
                int r = in.read(payload, read, payload.length - read);
                if (r < 0)
                    return null;
                read += r;
            }

            // Unmask
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskKey[i % 4];
                }
            }

            return new String(payload, StandardCharsets.UTF_8);
        }

        /** Send a text frame to this client. */
        synchronized void sendText(String text) {
            if (!handshakeDone || socket.isClosed())
                return;
            try {
                byte[] payload = text.getBytes(StandardCharsets.UTF_8);
                ByteArrayOutputStream frame = new ByteArrayOutputStream();

                frame.write(0x81); // FIN + opcode TEXT

                if (payload.length <= 125) {
                    frame.write(payload.length);
                } else if (payload.length <= 65535) {
                    frame.write(126);
                    frame.write((payload.length >> 8) & 0xFF);
                    frame.write(payload.length & 0xFF);
                } else {
                    frame.write(127);
                    long len = payload.length;
                    for (int i = 7; i >= 0; i--) {
                        frame.write((int) ((len >> (i * 8)) & 0xFF));
                    }
                }

                frame.write(payload);
                out.write(frame.toByteArray());
                out.flush();
            } catch (IOException e) {
                // Client gone
            }
        }

        // ── HTTP → WebSocket upgrade handshake ────────────────────────────

        private boolean doHandshake(InputStream in, OutputStream out) throws IOException {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));

            // Read HTTP request headers
            Map<String, String> headers = new LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(
                            line.substring(0, colon).trim().toLowerCase(),
                            line.substring(colon + 1).trim());
                }
            }

            String wsKey = headers.get("sec-websocket-key");
            if (wsKey == null)
                return false;

            // Compute accept token
            String acceptToken = computeAcceptToken(wsKey);

            // Send 101 Switching Protocols
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptToken + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return true;
        }

        private String computeAcceptToken(String key) {
            try {
                String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] sha1 = md.digest(magic.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(sha1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
