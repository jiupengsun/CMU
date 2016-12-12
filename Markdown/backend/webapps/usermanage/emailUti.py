import logging
from django.core.mail import send_mail

logger = logging.getLogger(__name__)

# send email to a specific destination address
def sendEmail(dest_addr, subject_text, message_text):

    sender = "Go Markdown"

    try:
      send_mail(
        subject_text,
        message_text,
        sender,
        [dest_addr],
        fail_silently=False,
      )

      return True
    except Exception as e:
      logger.error(e)
      return False

