import time
import argparse
import requests


def setup_argparse():
    parser = argparse.ArgumentParser()
    parser.add_argument("-keycloak_endpoint", required=True)
    parser.add_argument("-realm", required=True)
    parser.add_argument("-client_id", required=True)
    parser.add_argument("-client_secret", required=True)
    parser.add_argument("-username", required=True)
    parser.add_argument("-password", required=True)
    parser.add_argument("-service_endpoint", required=True)
    return parser.parse_args()


def generate_token_data(client_id, client_secret, username, password):
    return {
        'client_id': client_id,
        'grant_type': 'password',
        'client_secret': client_secret,
        'scope': 'openid',
        'username': username,
        'password': password
    }


def generate_token(keycloak_endpoint, realm, data):
    print(f"Generating token")
    url = f"{keycloak_endpoint}/auth/realms/{realm}/protocol/openid-connect/token"
    r = requests.post(url, data=data)
    r.raise_for_status()
    token = r.json()['access_token']
    print(f"{token}")
    return token


def call_microservice(service_endpoint, access_token):
    # print(f"Calling {args.service_endpoint}")
    r = requests.get(service_endpoint, headers={'Authorization': 'Bearer ' + access_token})
    print(f"Got response code {r.status_code}")
    if (r.status_code != 200):
        print(f"Got response headers {r.headers}")
    return r


def loop(init_access_token, level):
    access_token = init_access_token or generate_token(args.keycloak_endpoint, args.realm, token_data)
    print(f"\n============ Level {level} ============")
    while True:
        r = call_microservice(args.service_endpoint, access_token)


if __name__ == '__main__':
    args = setup_argparse()
    token_data = generate_token_data(args.client_id, args.client_secret, args.username, args.password)
    loop(None, 1)
