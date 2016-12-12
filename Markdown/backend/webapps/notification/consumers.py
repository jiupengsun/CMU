import logging
from django.contrib.auth.models import User

from channels.generic.websockets import JsonWebsocketConsumer
from django.db.models import Q
from Helper.JSON import *
from Helper.Util import *
from markdown.models import Document
from valid import *
from Helper.Constant import *

from notification.models import Notification

logger = logging.getLogger(__name__)

# dictionary stores user channel
user_channels = {}

class MyConsumer(JsonWebsocketConsumer):

  http_user = True

  def connect(self, message, **kwargs):
    user = self.message.user
    if not user.is_authenticated:
      self.close()
    # add user channel into map
    user_channels[str(user.id)] = self
    # return unread message
    notification = Notification.objects.filter(Q(user_receive=user) & Q(has_read='0'))
    if notification:
      list = []
      # has unread messages
      for unread in notification:
        list.append(notifyToJson(unread))
      self.send(getSuccessJsonObject(list, "000"))

    return

  def receive(self, content, **kwargs):
    user = self.message.user
    if not user.is_authenticated:
      self.close()

    valid = ValidNotifyWSMessage()
    if not valid.is_valid(content):
      # not valid check
      self.send(getErrorJsonObject(valid.error))
      return
    logger.debug(user.username + " send a notification, type" + valid.cleaned_data["message_type"])
    self.processMessage(valid.cleaned_data)

    return


  def disconnect(self, message, **kwargs):
    user = self.message.user
    if user.is_authenticated:
      try:
        # remove user channel from map
        del user_channels[str(user.id)]
      finally:
        return
    return

  # user accept the invitation
  def processMessage(self, cleaned_data):
    user = self.message.user
    # get receiver
    other_user = cleaned_data["other_user"]
    # message type
    type = cleaned_data["message_type"]
    try:
      if type != INVITATION:
        # not an invited message
        # then must contain nid
        notify = cleaned_data["notification"]
        document = notify.document
        notify.has_read = '1'
        notify.save()

        if type == HASREAD:
          # enough
          self.send(getSuccessJsonObject(notifyResponseJson(user, notify), "000"))
          return
        # user accept the request
        # then add him into shared user
        elif type == ACCEPT:
          # add user into shared_user
          document.shared_user.add(user)

      else:
        # invited message
        # then should contain docid
        docid = cleaned_data["docid"]
        document = Document.objects.filter(Q(id=docid) & Q(creator=user) & Q(is_delete='0'))
        if not document:
          # cannot find document
          self.send(getErrorJsonObject("110"))
          return
        document = document[0]

      new_notify = Notification.objects.create(
        user_send = user,
        user_receive = other_user,
        document = document,
        message_type = type
      )
      new_notify.save()
      list = []
      list.append(notifyToJson(new_notify))
      other_channel = user_channels[str(other_user.id)]
      if other_channel:
        # other user is online
        other_channel.send(getSuccessJsonObject(list, "000"))
      if type == INVITATION:
        self.send(getSuccessJsonObject({}, "000"))
      else:
        # need to response to client
        self.send(getSuccessJsonObject(notifyResponseJson(user, cleaned_data["notification"]), "000"))
    except Exception as e:
      logger.error(e)
      self.send(getErrorJsonObject("999"))

    return

