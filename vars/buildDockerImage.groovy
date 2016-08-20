def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def imageName = config.imageName
  def directory = config.directory

  stage "Build Docker image: ${imageName}"
  dir(directory) {
    return docker.build(imageName)
  }
}
