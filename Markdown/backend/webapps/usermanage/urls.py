from django.conf.urls import url
from usermanage.views import *

urlpatterns = [
  # user sign up
  url(r'signup', signup),
  # user sign in
  url(r'signin', signin),
  # edit personal information
  # url(r'edit', edit),
  # logout
  url(r'logout', user_logout),
  url(r'islogin', is_login),
  url(r'activate', activate),
]
