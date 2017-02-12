#!/usr/bin/env groovy

def sendSlackMessage = {
  msg = '', msgColor = 'good', credentialsId = '29731667-bfb1-433d-8dc1-f76d2a69c226' ->
    slackSend message: msg, color: msgColor, tokenCredentialId: credentialsId
}

def pushDockerImage = {
  image,
  credentialId = '54154007-6bac-4f89-be72-c253834b539a',
  dockerRegistry = '' ->
    docker.withRegistry(dockerRegistry, credentialId) {
      image.push()
    }
}
