import { env } from 'process'
import express from 'express'
import bodyParser from 'body-parser'
import { MongoClient } from 'mongodb'

const port = 'SERVICE_PORT' in env ? env.SERVICE_PORT : 8080
const hostname = 'HOSTNAME' in env ? env.HOSTNAME : 'localhost'
const mongoHost = 'MONGO_HOST' in env ? env.MONGO_HOST : "localhost"
const mongoPort = 'MONGO_PORT' in env ? env.MONGO_PORT : 27017
const mongoUser = 'MONGO_USER' in env ? env.MONGO_USER : "admin"
const mongoPassword = 'MONGO_PASSWORD' in env ? env.MONGO_PASSWORD : "admin"
const mongoConnection = `mongodb://${mongoUser}:${mongoPassword}@${mongoHost}:${mongoPort}`;
const mongoClient = new MongoClient(mongoConnection);
const server = express()
const database = "city_twin"
const mainstaysCollection = "mainstays"
const resourcesCollection = "resources"

server.use(bodyParser.json())
server.use(bodyParser.urlencoded({ extended: false }))

server.get('/mainstay', async (req, res) => {
    try {
        await mongoClient.connect()
        let db = mongoClient.db(database);
        let collection = await db.collection(mainstaysCollection);
        let query = {address: req.query.address};
        let result = await collection.find(query).toArray();
        if (!result) res.send("Not found").status(404);
        else res.send(result).status(200);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    } finally {
        await mongoClient.close();
    }
});

server.post("/mainstay", async (req, res) => {
    try {
        await mongoClient.connect();
        let db = mongoClient.db(database);
        let collection = await db.collection(mainstaysCollection);
        let document = req.body;
        document.date = new Date();
        let result = await collection.insertOne(document);
        res.send(result).status(204);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    } finally {
        await mongoClient.close();
    }
});

server.get('/resource', async (req, res) => {
    try {
        await mongoClient.connect()
        let db = mongoClient.db(database);
        let collection = await db.collection(resourcesCollection);
        let query = {name: req.query.name};
        let result = await collection.find(query).toArray();
        if (!result) res.send("Not found").status(404);
        else res.send(result).status(200);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    } finally {
        await mongoClient.close();
    }
});

server.post("/resource", async (req, res) => {
    try {
        await mongoClient.connect();
        let db = mongoClient.db(database);
        let collection = await db.collection(resourcesCollection);
        let document = req.body;
        document.date = new Date();
        let result = await collection.insertOne(document);
        res.send(result).status(204);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    } finally {
        await mongoClient.close();
    }
});

console.log(`Service listening on ${hostname}:${port}`);
server.listen(port);
