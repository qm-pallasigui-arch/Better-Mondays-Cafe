import sqlite3, base64, hashlib

def check(username, password):
    conn = sqlite3.connect('data/coffee-cafe.db')
    cur = conn.cursor()
    cur.execute('SELECT password_hash FROM users WHERE username=?', (username,))
    r = cur.fetchone()
    if not r:
        print('NO_USER')
        return
    stored = r[0]
    print('stored=', stored)
    salt_b64, hash_b64 = stored.split(':')
    salt = base64.b64decode(salt_b64)
    expected = base64.b64decode(hash_b64)
    m = hashlib.sha256()
    m.update(salt)
    m.update(password.encode())
    print('MATCH' if m.digest() == expected else 'NOPE')
    conn.close()

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 3:
        print('usage: python check_pwd.py <username> <password>')
    else:
        check(sys.argv[1], sys.argv[2])
