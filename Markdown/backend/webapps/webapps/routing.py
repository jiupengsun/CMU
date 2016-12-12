from channels import route, route_class
from chat import consumers as chatConsumer
from notification import consumers as notifyConsumer

channel_routing = [
  #route_class(chatConsumer.MyConsumer),
  route_class(chatConsumer.MyConsumer, path=r'/doc/[0-9]+'),
  route_class(notifyConsumer.MyConsumer, path=r'/notify'),

]
