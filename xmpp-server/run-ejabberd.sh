#!/bin/sh

docker run --name ejabberd -d -p 5222:5222 -p 5269:5269 -p 5280:5280 -h "openfire.bitfactory.co.th" -e "XMPP_DOMAIN=openfire.bitfactory.co.th" -e "EJABBERD_ADMINS=admin@openfire.bitfactory.co.th:password" -e "EJABBERD_USERS=admin@openfire.bitfactory.co.th:password user1@openfire.bitfactory.co.th:password user2@openfire.bitfactory.co.th:password user3@openfire.bitfactory.co.th:password user4@openfire.bitfactory.co.th:password" -e "TZ=Asia/Bangkok"  -e "EJABBERD_STARTTLS=true" rroemhild/ejabberd