from __future__ import unicode_literals

from django.db import models
from markdown.models import Document
from django.contrib.auth.models import User

# Create your models here.
class Notification(models.Model):
    # user who sends notification
    user_send = models.ForeignKey(User, on_delete=models.CASCADE, related_name="user_send")
    # user who receive notification
    user_receive = models.ForeignKey(User, on_delete=models.CASCADE, related_name="user_receive")
    # document
    document = models.ForeignKey(Document, on_delete=models.CASCADE, related_name="doc_notify")
    # message type
    message_type = models.CharField(max_length=5)
    # create time
    timestamp = models.DateTimeField(auto_now=True)
    # if user has read message
    has_read = models.CharField(max_length=1, default='0')


