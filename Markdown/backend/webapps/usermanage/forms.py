import logging
from django import forms
from django.contrib.auth.models import User
from django.contrib.auth import authenticate, login
import re
from Helper.Constant import *

logger = logging.getLogger(__name__)

# form for user registration
class UserRegisterForm(forms.Form):
  username = forms.CharField(max_length=USERNMAE_LENGTH)
  nickname = forms.CharField(max_length=NICKNAME_LENGTH)
  email = forms.EmailField(max_length=EMAIL_LENGTH)
  password = forms.CharField(max_length=PASSWORD_LENGTH)
  confirm_password = forms.CharField(max_length=PASSWORD_LENGTH)
  avatar = forms.ImageField(required=False)

  def clean(self):
    cleaned_data = super(UserRegisterForm, self).clean()
    if self.errors:
      self.add_error("username", "113")
      return False

    # judge username
    if User.objects.filter(username=cleaned_data.get('username')):
      self.add_error('username', '102')
      return False

    # judge if username is legal
    regex = "^[0-9_a-zA-Z]+$"
    m = re.search(regex, cleaned_data.get("username"))
    if not m:
      self.add_error("username", "103")
      return False

    # judge if email has been registered
    if User.objects.filter(email=cleaned_data.get("email")):
      self.add_error("email", "104")
      return False

    # judge email format
    regex = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    m = re.search(regex, cleaned_data.get("email"))
    if not m:
      self.add_error("email", "105")
      return False

    # judge if password and repeat password are the same
    if cleaned_data.get("password") != cleaned_data.get("confirm_password"):
      self.add_error("confirm_password", "106")
      return False


# form for user login
class UserLoginForm(forms.Form):
  username = forms.CharField(max_length=USERNMAE_LENGTH)
  password = forms.CharField(max_length=PASSWORD_LENGTH)

  def __init__(self, *args, **kwargs):
    self.request = (kwargs.pop('request', None))
    super(UserLoginForm, self).__init__(*args, **kwargs)

  def clean(self):
    cleaned_data = super(UserLoginForm, self).clean()

    if self.errors:
      self.add_error("username", "113")
      return False

    user = authenticate(username=cleaned_data.get("username"),
                        password=cleaned_data.get("password"))

    if not user:
      self.add_error("username", "107")
      return False

    '''
    if user[0].is_active != 1:
      self.add_error("username", "108")
      return False
    '''

    # successful
    # login user object
    login(self.request, user)

class ActivateForm(forms.Form):
  username = forms.CharField(max_length=50)
  token = forms.CharField(max_length=50)

  def clean(self):
    cleaned_data = super(ActivateForm, self).clean()

    if self.errors:
      self.add_error("username", "113")
      return False

    try:
      user = User.objects.get(username=cleaned_data["username"])
      if user.userextend.token != cleaned_data["token"]:
        self.add_error("token", "123")
        return False

      user.is_active = 1
      user.save()

    except Exception as e:
      self.add_error("user", "107")
      logger.error(e)
      return False

    return cleaned_data

class ForgetForm(forms.Form):
  email = forms.EmailField(max_length=100)

  def clean(self):
    cleaned_data = super(ForgetForm, self).clean()

    if self.errors:
      self.add_error("username", "113")
      return False

    email = User.objects.filter(email=cleaned_data.get('email'))

    if not email:
      self.add_error("email", "124")
      return False

    return cleaned_data

class ResetPasswordForm(forms.Form):
  password = forms.CharField(max_length=50)
  repeatPassword = forms.CharField(max_length=50)

  def clean(self):
    cleaned_data = super(ResetPasswordForm, self).clean()

    if self.errors:
      self.add_error("username", "113")
      return False

    if cleaned_data.get('password') != cleaned_data.get('repeatPassword'):
      self.add_error('password', "106")
      return False

    return cleaned_data
