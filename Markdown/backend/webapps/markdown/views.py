from django.contrib.auth.decorators import login_required

from markdown.forms import*
from Helper.Util import *

from markdown.models import Document
import logging

from notification.models import Notification

logger = logging.getLogger(__name__)

def index(request):
  if request.method == "GET":
    return JsonResponse({})

def document(request):
  if request.method == 'GET':
    context = {
      'flag': 1,
      'message': 'success',
      'data': {}
    }
    return JsonResponse(context)

# create a new document
@login_required
def add(request):
  if request.method == "POST":
    try:
      docForm = CreateDocForm(request.POST)
      if not docForm.is_valid():
        logger.error(docForm.errors)
        return raiseError(collectFormErrors(docForm))

      # successfully valid
      user = request.user
      doc = Document.objects.create(title=docForm.cleaned_data["title"],
                                    creator=user)

      '''
      shared_users = docForm.cleaned_data.get("shared_user")
      if shared_users:
        for u in shared_users:
          su = User.objects.filter(username=u)
          if su:
            doc.shared_user.add(su[0])
      '''
      doc.save()
      logger.debug(user.username + " add a new document named " + docForm.cleaned_data['title'] + " successfully ")
      logger.debug(user.username + " create a room named contain the document " + docForm.cleaned_data['title'])
      return raiseSuccess(docToJson(doc), '000')
    except Exception as e:
      logger.error(e)
      return raiseError("999")
  return raiseError("121")

# list document
# search documents created by the user, or
# documents shared to this user
@login_required
def list(request):
  if request.method == "POST":
    try:
      user = request.user
      ownedlist = Document.objects.filter(Q(creator=user) & Q(is_delete='0')).order_by("-create_date")
      ownedlist_json = []
      for doc in ownedlist:
        ownedlist_json.append(simpledocToJson(doc))

      sharedlist = Document.objects.filter(Q(shared_user=user) & Q(is_delete='0')).order_by("-create_date")
      sharedlist_json = []
      for doc in sharedlist:
        sharedlist_json.append(simpledocToJson(doc))

      logger.debug(request.user.username + " get all the document he/she created/shared ")
      doc_list_json = DOCUMENT_LIST.copy()
      doc_list_json["owned"] = ownedlist_json
      doc_list_json["shared"] = sharedlist_json

      return raiseSuccess(doc_list_json, '000')
    except Exception as e:
      logger.error(e)
      return raiseError("999")
  return raiseError("121")

# enter into a document
@login_required
def get(request):
  if request.method == "POST":
    try:
      docForm = GetDocForm(request.POST)
      if not docForm.is_valid():
        logger.error(docForm.errors)
        return raiseError(collectFormErrors(docForm))

      logger.debug(request.user.username + " get all the document , the document id is " + request.docid)
      doc = docForm.cleaned_data.get('document')
      return raiseSuccess(docToJson(doc), '000')
    except Exception as e:
      logger.error(e)
      return raiseError("999")

  return raiseError("121")

# delete an document
@login_required
def delete(request):
  if request.method == "POST":
    try:
      user = request.user
      delForm = DeleteDocForm(request.POST)
      if not delForm.is_valid():
        logger.error(delForm.errors)
        return raiseError(collectFormErrors(delForm))

      docid = delForm.cleaned_data.get("docid")
      doc = Document.objects.filter( Q(id=docid) & Q(is_delete='0'))
      if doc:
        doc = doc[0]
        if doc.creator.id == user.id:
          # delete his own document
          doc.is_delete = '1'
          doc.save()
          # delete notification messages
          notification = Notification.objects.filter(document=doc)
          if notification:
            notification.delete()
          logger.debug(request.user.username + " delete document " + docid)
          return raiseSuccess({}, '000')

      # search shared document
      doc = Document.objects.filter( Q(shared_user__exact=user)
                                     & Q(id=docid) & Q(is_delete='0'))
      if doc:
        doc = doc[0]
        doc.shared_user.remove(user)
        doc.save()
        logger.debug(request.user.username + " remove shared document " + docid)
        return raiseSuccess({}, '000')

      return raiseError('112')
    except Exception as e:
      logger.error(e)
      return raiseError("999")

  return raiseError("121")

# edit document's information
@login_required
def edit(request):
  if request.method == "POST":
    try:
      user = request.user
      editForm = EditDocForm(request.POST)
      if not editForm.is_valid():
        logger.error(editForm.errors)
        return raiseError(collectFormErrors(editForm))

      docid = editForm.cleaned_data.get("docid")
      doc = Document.objects.filter( Q(id=docid) & Q(is_delete='0'))
      if doc:
        doc = doc[0]
        if doc.creator_id == user.id:
          # modify title
          if(editForm.cleaned_data.get("title")):
            doc.title = editForm.cleaned_data.get("title")

          '''
          # modify shared user list
          shared_users = editForm.cleaned_data.get("shared_user")
          if shared_users:
            for u in shared_users:
              su = User.objects.filter(username=u)
              if su:
                doc.shared_user.add(su[0])
          '''
          # clear shared user list
          doc.shared_user.clear()
          doc.save()
          # successful
          return raiseSuccess(simpledocToJson(doc), "000")

        else:
          # no priviledge
          return raiseError("111")
      else:
        # no document
        return raiseError("111")
    except Exception as e:
      logger.error(e)
      return raiseError("999")

  return raiseError("121")
