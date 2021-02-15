#!/bin/bash

echo "* Request for authorization"
RESULT=`curl --data "username=admin&password=admin&grant_type=password&client_id=admin-cli" http://localhost:8081/auth/realms/master/protocol/openid-connect/token`

echo "* Recovery of the token"
TOKEN=`echo $RESULT | sed 's/.*access_token":"//g' | sed 's/".*//g'`

echo "* Display token"
echo $TOKEN

echo " * user creation\n"
declare -a realms=("rayman" "globox")
for realm in ${realms[@]}; do
  curl -v http://localhost:8081/auth/admin/realms/$realm/users -H "Content-Type: application/json" -H "Authorization: bearer $TOKEN" --data '{"username":"test","firstName":"test","lastName":"test","email":"test@example.com","enabled":"true","credentials":[{"temporary":false,"type":"password","value":"test"}]}'
done
