#!/usr/bin/env groovy
package net.reynn

def sendSlackMessage = {
  msg = '', msgColor = 'good', credentialsId = '29731667-bfb1-433d-8dc1-f76d2a69c226' ->
    slackSend message: msg, color: msgColor, tokenCredentialId: credentialsId
}
