def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def imageName = config.imageName
  def credentialsId = config.credentialsId ?: '54154007-6bac-4f89-be72-c253834b539a',
  def dockerRegistry = config.registry ?: ''

  docker.withRegistry(dockerRegistry, credentialsId) {
    docker.image(imageName).push()
  }
}
