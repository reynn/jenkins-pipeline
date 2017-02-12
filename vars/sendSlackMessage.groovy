def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def msg = config.msg ?: '',
  def msgColor = config.msgColor ?: 'good',
  def credentialsId = config.credentialsId ?: '29731667-bfb1-433d-8dc1-f76d2a69c226'

  slackSend message: msg, color: msgColor, tokenCredentialId: credentialsId
}
