from django.contrib.auth.models import User

from django.db.models import Q

from Helper.Constant import *

import logging

from markdown.models import Document
from notification.models import Notification

logger = logging.getLogger(__name__)

class ValidNotifyWSMessage():
  error = ""
  cleaned_data = {}

  def is_valid(self, content):

    try:
      data = content["data"]
      message_type = data["message_type"]
      if not message_type in [INVITATION, ACCEPT, REJECT, HASREAD]:
        self.error = "126"
        return False

      self.cleaned_data["message_type"] = message_type
      if message_type == INVITATION:
        self.cleaned_data["docid"] = data["docid"]
        # check other user
        other_username = data["other_user"]
        other_user = User.objects.filter(username=other_username)
        if not other_user:
          self.error = "117"
          return False
        self.cleaned_data["other_user"] = other_user[0]
      else:
        nid = data["nid"]
        notify = Notification.objects.filter(id=nid)
        if not notify:
          self.error = "127"
          return False
        self.cleaned_data["notification"] = notify[0]
        if message_type == HASREAD:
          # enough check
          self.cleaned_data["other_user"] = None
          return True

        # check other user
        other_username = data["other_user"]
        # other_user is id
        other_user = User.objects.filter(id=other_username)
        if not other_user:
          self.error = "117"
          return False
        self.cleaned_data["other_user"] = other_user[0]

      return True
    except Exception as e:
      logger.error(e)
      self.error = "113"
      return False
