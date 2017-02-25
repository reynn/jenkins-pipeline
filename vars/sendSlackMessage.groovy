def call(msg, color, channel, domain, credentialsId) {

  msg = config.msg ?: ''
  color = config.color ?: 'good'
  channel = config.channel ?: ''
  domain = config.domain ?: ''
  credentialsId = config.credentialsId ?: 'slack-integration-token'

  try {
    slackSend message: msg, color: color, channel: channel, domain: domain, tokenCredentialId: credentialsId
  } catch (java.lang.NoSuchMethodError nsme) {
    println "Slack plugin does not appear to be installed on this Jenkins server."
  } catch (Exception e) {
    println "Unable to send notification to Slack."
    println "Exception details -----------------------"
    println e
    println "End exception details -------------------"
  }

}
