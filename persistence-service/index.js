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
const mongoConnectionString = `mongodb://${mongoUser}:${mongoPassword}@${mongoHost}:${mongoPort}`;
const mongoClient = new MongoClient(mongoConnectionString);
const server = express()
const mainstaysCollection = "mainstays"
const resourcesCollection = "resources"
server.use(bodyParser.json())
server.use(bodyParser.urlencoded({ extended: false }))
let mongoConnection;
try {
    mongoConnection = await mongoClient.connect();
} catch(e) {
    console.error(e);
}
const database = mongoConnection.db("city_twin")

server.get('/mainstays', async (req, res) => {
    try {
        let collection = await database.collection(mainstaysCollection);
        let query = {};
        let result = await collection.find(query).sort( { "time": -1, "_id": -1 } ).limit(10000).toArray();
        if (!result) res.send("Not found").status(404);
        else res.send(result).status(200);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    }
});

server.post("/mainstays", async (req, res) => {
    try {
        let collection = await database.collection(mainstaysCollection);
        let document = req.body;
        let result = await collection.insertOne(document);
        res.send(result).status(204);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    }
});

server.get('/resources', async (req, res) => {
    try {
        let collection = await database.collection(resourcesCollection);
        let query = {};
        let result = await collection.find(query).sort( { "time": -1, "_id": -1 } ).limit(10000).toArray();
        if (!result) res.send("Not found").status(404);
        else res.send(result).status(200);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    }
});

server.post("/resources", async (req, res) => {
    try {
        let collection = await database.collection(resourcesCollection);
        let document = req.body;
        let result = await collection.insertOne(document);
        res.send(result).status(204);
    } catch (e) {
        res.send(e.toString());
        console.log(e.toString());
    }
});

console.log(`Service listening on ${hostname}:${port}`);
server.listen(port);
