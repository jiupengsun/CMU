from SimpleWebSocketServer import SimpleWebSocketServer, WebSocket

clients = []
class SimpleChat(WebSocket):

    def handleMessage(self):
       print "get chat message: " + str(self.data)
       for client in clients:
           if client != self:
            client.sendMessage(self.data)

    def handleConnected(self):
       print self.address, 'chat connected'
#        for client in clients:
#           client.sendMessage(self.address[0] + u' - connected')
       clients.append(self)

    def handleClose(self):
       clients.remove(self)
       print self.address, 'chat closed'
#        for client in clients:
#           client.sendMessage(self.address[0] + u' - disconnected')

server = SimpleWebSocketServer('', 8888, SimpleChat)
server.serveforever()
