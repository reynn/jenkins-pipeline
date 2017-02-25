def call(msg = '', color = 'good', channel = '', domain = '', credentialsId = 'slack-integration-token') {

  try {
    slackSend channel: channel, color: color, message: msg, teamDomain: domain, tokenCredentialId: credentialsId
  } catch (java.lang.NoSuchMethodError nsme) {
    println "Slack plugin does not appear to be installed on this Jenkins server."
  } catch (Exception e) {
    println "Unable to send notification to Slack."
    println "Exception details -----------------------"
    println e
    println "End exception details -------------------"
  }

}
