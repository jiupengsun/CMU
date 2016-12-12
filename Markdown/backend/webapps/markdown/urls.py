from django.conf.urls import url
from markdown.views import *


urlpatterns = [
  url(r'csrf', index),
  url(r'doc', document),
  url(r'create', add),
  url(r'get', get),
  url(r'list', list),
  url(r'delete', delete),
  url(r'edit', edit),
]

