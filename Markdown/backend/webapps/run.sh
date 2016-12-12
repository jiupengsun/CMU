cd ../../../
git pull
git add .
git commit -m $1
git push origin master
cd -
sudo /bin/rm -rf /var/www/Markdown/*
sudo cp -r ../../frontend/* /var/www/Markdown/
sudo apachectl restart
