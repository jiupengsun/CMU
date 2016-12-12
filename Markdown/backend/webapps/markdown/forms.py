import json

from django import forms
from django.contrib.auth.models import User
from django.db.models import Q

from Helper.Constant import *
from markdown.models import Document
import logging
logger = logging.getLogger(__name__)

class CreateDocForm(forms.Form):
  title = forms.CharField(max_length=DOC_TITLE_LENGTH)

  def clean(self):
    cleaned_data = super(CreateDocForm, self).clean()
    if self.errors:
      self.add_error('title', '109')
      return False

    '''
    try:
      shared_users = json.loads(dict(self.data.lists())["shared_user"][0])
      if shared_users and shared_users.__len__ > 0:
        cleaned_data["shared_user"] = shared_users

    except Exception as e:
      logger.error(e)
    '''
    return cleaned_data


class GetDocForm(forms.Form):
  docid = forms.CharField()

  def clean(self):
    cleaned_data = super(GetDocForm, self).clean()
    if self.errors:
      self.add_error('docid', '110')
      return False

    try:
      doc = Document.objects.get(id=cleaned_data.get('docid'))
      cleaned_data['document'] = doc
    except Exception as e:
      self.add_error('docid', '111')
      return False

    return cleaned_data

class DeleteDocForm(forms.Form):
  docid = forms.CharField()

  def clean(self):
    cleaned_data = super(DeleteDocForm, self).clean()
    if self.errors:
      self.add_error('docid', '110')
      return False

    regex = "^[0-9]+$"
    import re
    m = re.search(regex, cleaned_data["docid"])
    if not m:
      self.add_error('docid', '119')
      return False

    return cleaned_data

# edit document request form
class EditDocForm(forms.Form):
  docid = forms.CharField()
  title = forms.CharField(required=False)

  def clean(self):
    cleaned_data = super(EditDocForm, self).clean()
    if self.errors:
      self.add_error('docid', '110')
      return False

    try:
      regex = "^[0-9]+$"
      import re
      m = re.search(regex, cleaned_data["docid"])
      if not m:
        self.add_error('docid', '119')
        return False

      # get shared_users json array
      shared_users = json.loads(dict(self.data.lists())["shared_user"][0])
      if shared_users and shared_users.__len__ > 0:
        cleaned_data["shared_user"] = shared_users

    except Exception as e:
      logger.error(e)

    return cleaned_data

