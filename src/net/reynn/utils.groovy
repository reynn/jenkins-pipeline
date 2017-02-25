package net.reynn

import jenkins.model.*
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

// ########################
// # Credential Utils
// ########################

enum CredentialTypes {
  usernamePassword (com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl),
  sshPrivateKey (com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey),
  stringCredentials (org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl)

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
  def folders = env.JOB_NAME ?: ""
  for(p in folders.split('/')) {
    for(n in getFolderCredentials(p)) {
      folderCreds << n
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

def isDebug() {
  return env.DebugMode?.toBoolean() ?: false
}

// only print message if we are currently in debug mode
def debugPrint(title, msgdata, debugMode=null) {
  if (debugMode == null) {
    debugMode = isDebug()
  }
  if (debugMode) {
    println "### \u001B[35mDebug output for $title\u001B[0m ###"
    if (msgdata instanceof Map) {
      for (data in msgdata) {
        println "### \u001B[35mDebug >>> ${data.key}: ${data.value}\u001B[0m"
      }
    } else {
      println msgdata
    }
    println "### \u001B[35mEnd Debug\u001B[0m ###"
  }
}
