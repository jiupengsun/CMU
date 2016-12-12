from __future__ import unicode_literals

from django.db import models
from django.utils import timezone
from django.contrib.auth.models import User
from markdown.models import Document
import datetime

# Create your models here.
'''
websocket test
'''
class ChatMessage(models.Model):
  room = models.ForeignKey(Document, related_name='messages')
  handle = models.ForeignKey(User, related_name='messages')
  message = models.TextField()
  timestamp = models.DateTimeField(default=timezone.now, db_index=True)
'''
websocket test
'''
