from datetime import datetime
from django.contrib.auth import REDIRECT_FIELD_NAME

from django.http import JsonResponse

from Helper.JSON import *
from Helper.ReturnMessage import *

# retrieve error object from Form
def collectFormErrors(form):
  for errField in form.errors:
    err = form.errors.get(errField)
    for e in err:
      return e


# return encapulated json response contain
# a specific error code
# errorcode is defined in ReturnMessage
def raiseError(errorcode):
  context = CONTEXT.copy()
  context['flag'] = errorcode
  if Message[errorcode]:
    context['message'] = Message[errorcode]
  else:
    context['message'] = Message['999']
  return JsonResponse(context)


# return an Error Json Object whose error code is errorcode
def getErrorJsonObject(errorcode):
  context = CONTEXT.copy()
  context['flag'] = errorcode
  if Message[errorcode]:
    context['message'] = Message[errorcode]
  else:
    context['message'] = Message['999']
  return context

# return encapsulated json response
def raiseSuccess(data, successcode):
  context = CONTEXT.copy()
  if Message[successcode]:
    context['message'] = Message[successcode]
    context['data'] = data
  else:
    context['message'] = Message['000']
    context['data'] = data

  return JsonResponse(context)

# return an object represents a successful json
def getSuccessJsonObject(data, successcode):
  context = CONTEXT.copy()
  if Message[successcode]:
    context['message'] = Message[successcode]
    context['data'] = data
  else:
    context['message'] = Message['000']
    context['data'] = data
  return context

# transform document object to json
def docToJson(document):
  if document:
    doc = DOCUMENT.copy()
    doc['docid'] = document.id
    doc['title'] = document.title
    doc['uid'] = document.creator.id
    doc['ctime'] = dateToStr(document.create_date)
    doc['content'] = document.content
    doc['ltime'] = dateToStr(document.last_saved_time)
    doc["shared"] = []
    for u in document.shared_user.all():
      doc["shared"].append(userToJson(u))
    return doc
  return None

# transform document object to json
def simpledocToJson(document):
  if document:
    doc = DOCUMENT_SIMPLE.copy()
    doc['docid'] = document.id
    doc['title'] = document.title
    doc['uid'] = document.creator.id
    doc['ctime'] = dateToStr(document.create_date)
    doc['ltime'] = dateToStr(document.last_saved_time)
    doc["shared"] = []
    for u in document.shared_user.all():
      doc["shared"].append(userToJson(u))
    return doc
  return None

# transform user object to json
def userToJson(user_obj):
  if user_obj:
    user = USER.copy()
    user['uid'] = str(user_obj.id)
    user['username'] = user_obj.username
    user['nickname'] = user_obj.userextend.nickname
    user['avatar'] = str(user_obj.userextend.avatar)
    return user
  return None

# transform date to json
def dateToStr(date):
  return date.strftime('%Y-%m-%d %H:%M:%S.%f')

# transform string to date
def strToDate(date_str):
  json_date = datetime.strptime(date_str, '%Y-%m-%d %H:%M:%S.%f')
  return json_date

def notificationToJSON(type, docid, user1, nid):
  notification = NOTIFICATION.copy()
  notification['content_type'] = type
  notification['docid'] = docid
  notification['other_uid'] = user1.id
  notification['nid'] = nid

def welcome(user):
  if user:
    welcome_json = WELCOME_MESSAGE.copy()
    welcome_json['uid'] = str(user.id)
    welcome_json["avatar"] = str(user.userextend.avatar)
    welcome_json['nickname'] = user.userextend.nickname
    return welcome_json

def notifyToJson(unread):
  if unread:
    notify = NOTIFICATION.copy()
    notify["user_send"] = userToJson(unread.user_send)
    notify["time"] = dateToStr(unread.timestamp)
    notify["type"] = unread.message_type
    notify["doc"] = docToJson(unread.document)
    notify["nid"] = str(unread.id)
    return notify
  return None

def notifyResponseJson(user, notify):
  list = []
  if user and notify:
    response = NOTIFICATION_RESPONSE.copy()
    response["message_type"] = notify.message_type
    response["other_user"] = str(user.id)
    response["nid"] = str(notify.id)
    list.append(response)
    return list
