package net.reynn

import jenkins.model.*;
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;

/***************************************
Credential Utils
***************************************/
enum CredentialTypes {
  usernamePassword (com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl),
  sshPrivateKey (com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey),
  stringCredentials (org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl),
  secretFile (org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl)

  final Class cValue

  CredentialTypes(Class value) {
    this.cValue = value
  }

  Class getValue() {
    return this.cValue
  }

  String getKey() {
    name()
  }

  static List getValues() {
    List l = []
    for (v in this.values()) {
      l << v.getValue()
    }
    return l
  }
}

/*
 * Get the credentials based on criteria defined in a map
 *
 * Map parameters:
 *
 *   @param class -  enum CredentialTypes of what kind of credential to search for
 *   @param id - jenkins id of the credential
 *   @param password - password of the credential
 *   @param description - description of the credential
 *
 * Other parameters can be passed and will be evaluated if they are properties of the credential type passed in the map
*/
def getCredentialsWithCriteria(criteria) {

  debugPrint("getCredentialsWithCriteria :: criteria", criteria)

  // Make sure properties isn't empty
  assert criteria : "No criteria provided."

  if (criteria.keySet().contains('class')) {
    assert criteria."class".class != java.lang.String : "java.lang.String is not a valid class for credentials"
    criteria."class" = criteria."class".class == CredentialTypes ? criteria."class"?.getValue() : criteria."class"
    assert criteria."class" in CredentialTypes.getValues() : "Credential type ${criteria.'class'} is not supported or is invalid."
  }

  // Number of properties that that are in the map
  def count = criteria.keySet().size()
  def credentials = []

  // Get all of the global credentials
  def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
      com.cloudbees.plugins.credentials.impl.BaseStandardCredentials.class,
      jenkins.model.Jenkins.instance)
  // Get credentials for the folder that the job is in
  java.util.ArrayList folderCreds = new java.util.ArrayList()
  def folders = (env.JOB_NAME ?: "").split('/')
  for (i = 0; i < folders.size(); i++) {
    folderName = folders[0..i].join('/')
    for(cred in getFolderCredentials(folderName)) {
      folderCreds << cred
      debugPrint("getCredentialsWithCriteria :: Folder Credentials: ", folderCreds)
    }
  }
  // Separately loop through credentials provided by different credential providers
  for(s in [folderCreds, creds]) {
    // Filter the results based on description and class
    for (c in s) {
      def i = 0
      if(count == c.getProperties().keySet().intersect(criteria.keySet()).size()) {
        if(c.getProperties().keySet().intersect(criteria.keySet()).equals(criteria.keySet())) {
          for ( p in c.getProperties().keySet().intersect(criteria.keySet())) {
            if (c."${p}" != criteria."${p}") {
              break;
            } else {
              i++;
            }
          }
        }
      }
      if (i == count) {
        credentials << c
      }
    }
  }
  // Fail if no credentials are found that match the criteria
  assert credentials : """No credentials found that match your criteria: ${criteria}"""
  assert credentials.size() == 1 :  """
  ${
    println "Multiple credentials found for search criteria.\n"
    println "Criteria:"
    println criteria
    println ""
    println "Credentials:"
    for(l in credentials) {
      println "id: ${l.id} description: ${l.description}"
    }
  }
  """
  // Get the single credential
  def credential = credentials[0]
  assert credential.id : "Invalid credentials. The id property of your credential is blank or corrupted."
  // Return the credentials
  return credential
}

def addCredential(folderName, type, credentialData) {
  def folder = findFolder(folderName)
  assert folder : "Specified folder not found"

  def credId = java.util.UUID.randomUUID().toString()
  Credentials cred = null
  switch(type) {
    case 'sshPrivateKey':
      cred = (Credentials)new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        credId,
        credentialData?.username,
        credentialData?.privateKeySource,
        credentialData?.passphrase,
        credentialData?.description)
    break;
    case 'usernamePassword':
      cred = (Credentials)new UsernamePasswordCredentialsImpl(
        CredentialsScope.GLOBAL,
        credId,
        credentialData?.description,
        credentialData?.username,
        credentialData?.password)
    break;
    case 'stringCredentials':
      cred = (Credentials)new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        credId,
        credentialData?.description,
        new hudson.util.Secret(credentialData?.secret))
    break;
    case 'secretFile':
      cred = (Credentials)new FileCredentialsImpl(
        CredentialsScope.GLOBAL,
        credId,
        credentialData?.description,
        credentialData?.fileName,
        SecretBytes.fromString(credentialData?.secretBytes))
    break;
  }
  assert cred : "Unable to create credential based on data provided"
  def credProperty = getFolderCredentialProperty(folder)
  credProperty.getStore().addCredentials(Domain.global(), cred)
}

def getFolderCredentialProperty(folder) {
  def property = folder.getProperties().get(FolderCredentialsProperty.class)
  if(!property) {
    println "Initialize Folder Credentials store and add credentials in global store"
    property = new FolderCredentialsProperty()
    folder.addProperty(property)
  }
  return property
}

def findFolder(folderName) {
  println "Finding folder: ${folderName}"
  def retFolder = null
  def parent = null
  def name = null
  if (folderName.contains('/')) {
    debugPrint("findFolder :: splitFolderName :: ${folderName}", "")
    def nameSplit = folderName.split('/')
    parent = nameSplit[0]
    name = nameSplit[1]
    println "FolderSplit(${nameSplit})"
  } else {
    debugPrint("findFolder :: noSplitFolderName :: ${folderName}", "")
    name = folderName
  }
  for (folder in Jenkins.getInstance().getAllItems(Folder.class)) {
    if (folder.name.equals(name)) {
      debugPrint("findFolder :: nameMatch :: ${folder.name}", "")
      if (parent) {
        if (parent instanceof com.cloudbees.hudson.plugins.folder.Folder && parent.name.equals(parent)) {
          debugPrint("findFolder :: parentMatch :: ${folder.name}", "")
          return folder
        } else {
          debugPrint("findFolder :: noParentMatch :: ${folder.name}", "")
        }
      } else {
        debugPrint("findFolder :: noParent :: ${folder.name}", "")
        return folder
      }
    } else {
      debugPrint("findFolder :: noMatch :: ${folder.name}", "")
    }
  }
  return retFolder
}

/***************************************
General
***************************************/
def isDebug() {
  return env.DebugMode?.toBoolean() ?: false
}

def centerPrint(text, length=120, ch='#') {
  println " ${text} ".center(length, ch)
}

// only print message if we are currently in debug mode
def debugPrint(title, msgdata, debugMode=null) {
  if (debugMode == null) {
    debugMode = isDebug()
  }
  if (debugMode) {
    println "### \u001B[35mDebug output for $title\u001B[0m ###"
    if (msgdata) {
      if (msgdata instanceof Map) {
        for (data in msgdata) {
          println "### \u001B[35mDebug >>> ${data.key}: ${data.value}\u001B[0m"
        }
      } else {
        println msgdata
      }
    }
    println "### \u001B[35mEnd Debug\u001B[0m ###"
  }
}

/***************************************
File Utilities
***************************************/
def getFileBytes(filePath) {
  assert filePath : "Empty filePath unacceptable"
  def fileExists = fileExists filePath
  if (!fileExists) { error("File :: (${filePath}) does not exist") }
  def fileContents = readFile filePath
  return fileContents.getBytes()
}

def getFileSecretBytes(filePath) {
  def fileBytes = getFileBytes(filePath)
  return SecretBytes.fromBytes(fileBytes)
}

/***************************************
Job Utilities
***************************************/
def getBuildCause(causeClass) {
  if (causeClass) {
    return currentBuild.rawBuild.getCause(causeClass)
  } else {
    return currentBuild.rawBuild.getCauses()
  }
}

def getUpstreamJobName(upstreamCause=null) {
  if (!upstreamCause) {
    upstreamCause = getBuildCause(hudson.model.Cause.UpstreamCause.class)
  }
  assert upstreamCause : "Job is not upstream"
  return upstreamCause.getUpstreamProject()
}

/***************************************
Parsing
***************************************/
def parseJSON(content) {
  assert content : "parseJSON :: Unable to parse empty content."
  def json = readJSON text: content
  return json
}

def parseYAML(content) {
  assert content : "parseYAML :: Unable to parse empty content."
  def yml = readYaml text: content
  return yml
}

def toJSON(contentMap) {
  assert contentMap : "No Map provided to convert to JSON"
  def jsonOutput = groovy.json.JsonOutput.toJson(contentMap)
  return jsonOutput
}

def toYAML(contentMap) {
  error("Not implemented yet.")
}

/***************************************
Replacements
***************************************/
def getEnv() {
  return env
}
