
USERNMAE_LENGTH = 50
NICKNAME_LENGTH = 50
LAST_NAME_LENGTH = 50
PASSWORD_LENGTH = 50
EMAIL_LENGTH = 50
BIOGRAPHY_LENGTH = 300
TOKEN_LENGTH = 60
DOC_TITLE_LENGTH = 100

# redis
USER_CHANNEL_DICT = "USERS_CHANNEL"

# notification
INVITATION = "0"
ACCEPT = "1"
REJECT = "2"
HASREAD = "3"


# sample content
SAMPLE_CONTENT = "#### Welcome to the markdown collaboration tool\n\n##### This is what you can do\n(Developed without third-part library support)\n* Create List\n   * Or create nested list by adding three whitespace (each nest level) at the beginning\n      * Like this\n* Inline syntax such as:\n   * **Bold** syntax\n   * __Underline__\n   * --Delete Line--\n   * [Hyper Text](google.com)\n* You can add a code block like this\n''' java\npublic static void resetTheWorld(int theAnswer){\n    int a = theAnswer;\n    return -a;\n}\n'''\n* We also support Image\n* ![](https://media.giphy.com/media/9gISqB3tncMmY/giphy.gif)\n\n(You can clear these text by click [this link](reset))\n\nFinally\n\n# Good job\n\n![](https://media.giphy.com/media/26ufkNFQ5mFJHeTu0/giphy.gif)"