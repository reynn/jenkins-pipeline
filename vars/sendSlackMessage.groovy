def call(msg, color, channel, domain, credentialsId) {

  def msg = config.msg ?: '',
  def color = config.color ?: 'good',
  def channel = config.channel ?: '',
  def domain = config.domain ?: '',
  def credentialsId = config.credentialsId ?: 'slack-integration-token'

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
