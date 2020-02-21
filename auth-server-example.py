import http.server
import socketserver
import json
from dataclasses import dataclass


@dataclass
class User:
    id: str
    email: str
    password: str
    first_name: str
    last_name: str
    gender: str


users = [
    User('1', 'johndoe@example.com', 'blackcat', 'John', 'Doe', 'm'),
    User('2', 'mikejohnson@example.com', 'redcar', 'Mike', 'Johnson', 'm'),
    User('3', 'rosejohns@example.com', 'whitepillow', 'Rose', 'Johns', 'f'),
]


class Handler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        body = json.loads(body)
        response = None
        for user in users:
            if body['username'] == user.email and body[
                    'password'] == user.password:
                response = {
                    'userId': user.id,
                    'firstName': user.first_name,
                    'lastName': user.last_name,
                    'email': user.email,
                    'gender': user.gender
                }
                break

        if response:
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            body = json.dumps(response).encode()
            self.send_header('Content-Length', len(body))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_response(401)
            self.end_headers()
        return


httpd = socketserver.TCPServer(('', 9090), Handler)
print('Started server on port 9090')
httpd.serve_forever()
