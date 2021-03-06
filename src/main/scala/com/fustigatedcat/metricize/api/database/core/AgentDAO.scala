package com.fustigatedcat.metricize.api.database.core

import com.fustigatedcat.metricize.api.model._
import org.apache.commons.lang3.RandomStringUtils
import org.json4s._
import org.json4s.JsonDSL._

import scala.slick.driver.MySQLDriver.simple._

object AgentDAO {

  val agents = TableQuery[Agents]

  val mysqlagentconfigs = TableQuery[MYSQLAgentConfigs]

  val postgresagentconfigs = TableQuery[POSTGRESAgentConfigs]

  def getAgentByKey(key : String) : Option[Agent] = DB.db.withSession { implicit session =>
    agents.filter(_.agentKey === key.value).firstOption
  }

  def getMysqlConfig(id : Long)(implicit session : Session) : Option[MYSQLAgentConfig] = {
    mysqlagentconfigs.filter(_.agentId === id).firstOption
  }

  def getPostgresConfig(id : Long)(implicit session : Session) : Option[POSTGRESAgentConfig] = {
    postgresagentconfigs.filter(_.agentId === id).firstOption
  }

  def mysqlConfigToJValue(config : Option[MYSQLAgentConfig]) : JValue = config match {
    case Some(mysqlConfig) => {
      ("mysqlAgentConfigId" -> mysqlConfig.mysqlAgentConfigId) ~
        ("fqdn" -> mysqlConfig.fqdn) ~
        ("port" -> mysqlConfig.port) ~
        ("username" -> mysqlConfig.username) ~
        ("password" -> mysqlConfig.password) ~
        ("queryString" -> mysqlConfig.queryString) ~
        ("countPer" -> mysqlConfig.countPer) ~
        ("timeUnit" -> mysqlConfig.timeUnit) ~
        ("dbName" -> mysqlConfig.dbName)
    }
    case _ => JObject()
  }

  def postgresConfigToJValue(config : Option[POSTGRESAgentConfig]) : JValue = config match {
    case Some(postgresConfig) => {
      ("postgresAgentConfigId" -> postgresConfig.postgresAgentConfigId) ~
        ("fqdn" -> postgresConfig.fqdn) ~
        ("port" -> postgresConfig.port) ~
        ("username" -> postgresConfig.username) ~
        ("password" -> postgresConfig.password) ~
        ("queryString" -> postgresConfig.queryString) ~
        ("countPer" -> postgresConfig.countPer) ~
        ("timeUnit" -> postgresConfig.timeUnit) ~
        ("dbName" -> postgresConfig.dbName)
    }
    case _ => JObject()
  }

  val agentConfigs : Map[Symbol, (Long, Session) => JValue] = Map(
    'NONE -> ((id, session) => JObject()),
    'MYSQL -> ((id, session) => mysqlConfigToJValue(getMysqlConfig(id)(session))),
    'POSTGRES -> ((id, session) => postgresConfigToJValue(getPostgresConfig(id)(session)))
  )

  def agentToJValue(agent : Agent) : JValue = DB.db.withSession { implicit session =>
    ("id" -> agent.id) ~
      ("customerId" -> agent.customerId) ~
      ("name" -> agent.name) ~
      ("agentKey" -> agent.agentKey) ~
      ("agentType" -> agent.agentType) ~
      ("config" -> agentConfigs(Symbol(agent.agentType.getOrElse("NONE")))(agent.id.get, session))
  }

  def createAgent(customer : Customer, name : String) = DB.db.withSession { implicit session =>
    val a = Agent(None, Some(customer.id.get), name, Some(RandomStringUtils.randomAlphabetic(512)), Some("NONE"))
    val agentId = agents.returning(agents.map(_.id)) += a
    Agent(Some(agentId), a.customerId, a.name, a.agentKey, a.agentType)
  }

}
