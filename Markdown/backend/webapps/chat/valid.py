from django.db.models import Q

from markdown.models import Document
from django.contrib.auth.models import User
import logging
logger = logging.getLogger(__name__)

class ValidDocWSMessage():
  error = ""
  cleaned_data = {}

  def is_valid(self, content):
    try:
      data = content["data"]
      action = data["action"]
      action = action.strip("/").split("/")

      # valid user
      userid = data["uid"]
      user = User.objects.filter(id=userid)
      if not user:
        self.error = "117"
        return False
      self.cleaned_data["user"] = user[0]

      # valid document
      docid = data["docid"]
      doc = Document.objects.filter(Q(id=docid) & (Q(creator=user) | Q(shared_user=user)))
      if not doc:
        self.error = "111"
        return False
      # document has been deleted
      if doc[0].is_delete == '1':
        self.error = "122"
        return False
      self.cleaned_data["docid"] = docid
      self.cleaned_data["document"] = doc[0]

      self.cleaned_data["content"] = data["content"]

      # valid action
      # need to change this to regex expression
      if len(action) == 1:
        action = action[0]
        if action == "chat":
          self.cleaned_data['action'] = action
          self.cleaned_data["nickname"] = user[0].userextend.nickname
          return True
        # add or remove shared users
        elif action in ["addUser", "delUser"]:
          self.cleaned_data['action'] = action
          self.cleaned_data["shared_user"] = data["shared_user"]
          # enough checking
          return True
        else:
          raise Exception

      elif len(action) == 2:
        action = action[1]
        if not action in ["init", "add", "delete", "update"]:
          self.error = "112"
          return False
        self.cleaned_data["action"] = action

      else:
        raise Exception

      # enough checking for init
      if action=="init":
        return True

      # check other parameters
      start = data["start"]
      end = data["end"]
      self.cleaned_data["start"] = int(start)
      self.cleaned_data["end"] = int(end)

      return True

    except Exception as e:
      logger.error(e)
      self.error = "113"
      return False

class ValidNotifyWSMessage():
  error = ""
  cleaned_data = {}

  def is_valid(self, content):

    try:

      return
    except Exception as e:
      logger.error(e)
      self.error = "113"
      return False

    return True