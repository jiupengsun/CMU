from django.db import models
from django.contrib.auth.models import User

# Create your models here.

# Document model
from Helper import Constant


class Document(models.Model):
  # title
  title = models.CharField(max_length=100)
  # content
  content = models.TextField(default=Constant.SAMPLE_CONTENT)
  # create time
  create_date = models.DateTimeField(auto_now=True)
  # creator/owner
  creator = models.ForeignKey(User, on_delete=models.CASCADE, related_name='create_doc')
  # last saved time
  last_saved_time = models.DateTimeField(auto_now=True)
  # shared user
  shared_user = models.ManyToManyField(User, related_name='shared_doc', blank=True)
  # if this doc has been deleted
  is_delete = models.CharField(max_length=1, default='0')


