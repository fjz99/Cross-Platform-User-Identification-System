from tornado.ioloop import IOLoop
from tornado.iostream import StreamClosedError
from tornado.tcpserver import TCPServer


class EchoServer(TCPServer):
    async def handle_stream(self, stream, address):
        while True:
            try:
                data = await stream.read_until(b"\n")
                print('data', data)
                await stream.write(data)
                await stream.write(b'end')
                print('done')
            except StreamClosedError:
                break


if __name__ == '__main__':
    server = EchoServer()
    server.listen(8888)
    print('das')
    IOLoop.current().start()
