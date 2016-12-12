from __future__ import unicode_literals

from django.contrib.auth.models import User
from django.db import models
from markdown.models import Document
from Helper.Constant import *

# Create your models here.

# Create your models here.
class UserExtend(models.Model):
  # user object
  user = models.OneToOneField(User, on_delete=models.CASCADE)
  # nickname
  nickname = models.CharField(max_length=NICKNAME_LENGTH)
  # age
  age = models.CharField(max_length=5, null=True, default='')
  # self biography
  biography = models.CharField(max_length=BIOGRAPHY_LENGTH, null=True, default='')
  # user avatar
  avatar = models.ImageField(upload_to='user-avatars', default='user-avatars/default_avatar.png')
  # activated token
  token = models.CharField(max_length=TOKEN_LENGTH, null=True)
  # reverse data 1
  reserve1 = models.CharField(max_length=1, null=True)
  # reverse data 2
  reserve2 = models.CharField(max_length=50, null=True)
