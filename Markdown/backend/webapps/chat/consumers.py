
from channels import Group
from channels.generic.websockets import JsonWebsocketConsumer

from markdown.models import *
import logging

from django.db.models import Q

from Helper.Util import *
from markdown.models import Document
from chat.models import ChatMessage
from valid import ValidDocWSMessage

logger = logging.getLogger(__name__)

class MyConsumer(JsonWebsocketConsumer):

  http_user = True


  def connection_groups(self, **kwargs):
    return ["test"]

  # connect callback
  def connect(self, message, **kwargs):
    user = self.message.user
    if not user.is_authenticated:
      # user not login, then close the connection
      self.send(getErrorJsonObject('120'))
      self.close()
      return

    try:
      docid = self.path.strip('/').split("/")[1]

      # search if the docid exists or if the user has priviledge to access this document
      doc = Document.objects.filter(Q(id=docid) & (Q(creator=user) | Q(shared_user=user)))
      if not doc:
        # cannot find document
        # or user doesn't have priviledge
        self.send(getErrorJsonObject('111'))
        logger.error(user.username + " intend to add a document with id " + docid + " , but the document does not exist ")
        self.close()
        return

      doc = doc[0]
      if doc.is_delete == '1':
        # current document has been removed
        self.send(getErrorJsonObject("122"))
        self.close()
        return

      logger.info(str(user.id) + " connect the room ")

      channel_id = 'doc-' + str(doc.id)

      Group(channel_id).add(message.reply_channel)
      #Group(channel_id).send({"text": json.dumps(docToJson(doc))})
      # send document information
      context = docToJson(doc)
      context["action"] = "doc/init"
      self.send(getSuccessJsonObject(context, "000"))
      # send welcome message to group
      self.group_send(channel_id, getSuccessJsonObject(welcome(user), "000"))
      message.channel_session['room'] = str(doc.id)
      return
    except Exception as e:
      logger.error(e)
      self.send(getErrorJsonObject("999"))



  def receive(self, content, **kwargs):
    # check user
    user = self.message.user
    if not user.is_authenticated:
      self.send(getErrorJsonObject("120"))
      self.close()
      return

    try:
      if not content:
        self.send(getErrorJsonObject("113"))
        logger.debug("malformed request")
        return

      valid = ValidDocWSMessage()
      if not valid.is_valid(content):
        # request data is malformed
        self.send(getErrorJsonObject(valid.error))
        return

      action = valid.cleaned_data["action"]
      # transform request according to different request action
      if action == "chat":
        return self.ws_chat(content, valid.cleaned_data)
      elif action == "addUser" or action == "delUser":
        return self.ws_share(content, valid.cleaned_data)
      else:
        return self.ws_doc(content, valid.cleaned_data)
    except Exception as e:
      logger.error(e)
      self.send(getErrorJsonObject("999"))



  def disconnect(self, message, **kwargs):
    #user = self.message.user
    docid = self.path.strip('/').split("/")[1]

    # remove channel from group
    Group('doc-' + docid).discard(message.reply_channel)

    return

  # add or remove shared users
  def ws_share(self, content, cleaned_data):
    action = cleaned_data["action"]
    shared_user = cleaned_data["shared_user"]
    document = cleaned_data["user"]
    try:
      for uid in shared_user:
        # check user
        user = User.objects.get(id=uid)
        if action == "addUser":
          document.shared_user.add(user)
          # here need send notification to specific user
        else:
          # send notification to specific user
          document.shared_user.remove(user)

      return raiseSuccess({}, "000")
    except Exception as e:
      return raiseError("117")

  # process chat request
  def ws_chat(self, content, cleaned_data):

    ChatMessage.objects.create(room=cleaned_data["document"],
                                   handle=cleaned_data["user"],
                                   message=cleaned_data["content"])
    user = self.message.user

    # add avatar path into request
    content['data']['avatar'] = str(user.userextend.avatar)
    # send to group
    self.group_send("doc-"+cleaned_data["docid"], content)
    return

  # process document operation request
  def ws_doc(self, content, cleaned_data):

    doc = cleaned_data["document"]
    ori_content = doc.content
    new_content = cleaned_data["content"]
    action = cleaned_data["action"]
    if action=="init":
      # initialization
      self.send(docToJson(doc))
      return
    start = cleaned_data["start"]
    end = cleaned_data["end"]
    if action=="add":
      ori_content = ori_content[0: start] + new_content + ori_content[start :]
    elif action=="update":
      ori_content = ori_content[0: start] + new_content + ori_content[end :]
    else: # delete
      ori_content = ori_content[0: start] + ori_content[end : ]

    # save into database
    doc.content = ori_content
    doc.save()
    # send to group
    self.group_send("doc-"+str(doc.id), content)

    return
