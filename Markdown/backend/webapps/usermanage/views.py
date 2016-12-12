import json

from django.contrib.auth import logout
from django.contrib.auth.decorators import login_required

from django.db import transaction

from Helper.Util import *
from usermanage.emailUti import sendEmail
from usermanage.forms import *
from usermanage.models import *

import binascii, os
import logging
from webapps.settings import EMAIL_ENABLED

logger = logging.getLogger(__name__)
# Create your views here.

@transaction.atomic
def signup(request):
  # serve for post method
  if request.method == "POST":
    regForm = UserRegisterForm(request.POST, request.FILES)
    if not regForm.is_valid():
      logger.error(regForm.errors)
      return raiseError(collectFormErrors(regForm))

    # successfully valid
    uid =transaction.savepoint()
    logger.info(regForm.cleaned_data['username'] + " register without email confirmation ")

    try:
      # adding email confirmation here
      token = str(binascii.hexlify(os.urandom(TOKEN_LENGTH / 2)))
      if EMAIL_ENABLED:
        active = 0
      else:
        active = 1
      user = User.objects.create_user(
        username=regForm.cleaned_data.get("username"),
        email=regForm.cleaned_data.get("email"),
        is_active=active,
        password=regForm.cleaned_data.get("password"),
      )
      userExtend = UserExtend.objects.create(user=user,
                                             nickname=regForm.cleaned_data.get("nickname"),
                                             token=token,
                                             )
      if regForm.cleaned_data.get("avatar"):
        userExtend.avatar = regForm.cleaned_data.get("avatar")
      userExtend.save()
      if EMAIL_ENABLED and (not send_register_email(user, token)):
        # send email to user
        transaction.savepoint_rollback(uid)
        return raiseError("128")
      logger.info(regForm.cleaned_data['username'] + " successful register and email confirmation ")
      return raiseSuccess(userToJson(user), '001')

    except Exception as e:
      # rollback database
      logger.error(e)
      return raiseError('999')

# user signin
def signin(request):
  if request.method == "POST":
    logForm = UserLoginForm(request.POST, request=request)
    if not logForm.is_valid():
      logger.error(logForm.errors)
      return raiseError(collectFormErrors(logForm))
    else:
      return raiseSuccess(userToJson(request.user), '000')


@login_required
def user_logout(request):
  logout(request)
  logger.debug(request.user.username + " log out  ")
  return raiseSuccess({}, '000')

# judge if user has logon
def is_login(request):
  user = request.user
  if user.is_authenticated:
    return raiseSuccess(userToJson(user), "000")
  else:
    return raiseError("120")

def send_register_email(user, token):
  url = "http://52.87.188.28/user/activate?username=" + user.username + "&token=" + token
  subject_text = "[Go Markdown]Activate your account!"
  message_text = "Dear " + user.first_name + ":\n" \
                 + "Welcome to Go Markdown!\n" \
                 + "Please activate your account by click the following link:" + url
  return sendEmail(user.email, subject_text, message_text)

# activate account
def activate(request):
  if request.method == "GET":
    actForm = ActivateForm(request.GET)
    if actForm.is_valid():
      # successfully
      message = "Activated Succesfully! Now click <a href='/signin.html'>Here</a>" \
                + " to log in"
      return raiseSuccess({}, "000")

    else:
      return raiseError(collectFormErrors(actForm))

# forget password page
def forget(request):
  if request.method == "POST":
    forf = ForgetForm(request.POST)

    if not forf.is_valid():
      return raiseError(collectFormErrors(forf))

    # pass validation
    user = User.objects.get(email__exact=forf.cleaned_data.get('email'))
    ue = user.userextend
    token = str(binascii.hexlify(os.urandom(30)))
    ue.token = token
    ue.save()

    message = "Please log into your email, and click the <a href='http://52.87.188.28/user/reset?username=" \
              + user.username + "&token=" + token + "'>link</a> to change your password"

    if sendEmail(user.email, "Please reset your password", message):
      # jump to activate page
      return raiseSuccess({}, "000")

    else:
      return raiseError("125")

