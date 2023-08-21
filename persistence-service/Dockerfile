FROM node:20-alpine
COPY ./ $HOME/persistence-service
WORKDIR $HOME/persistence-service
ENV SERVICE_PORT 80
ENV MONGO_USER admin
ENV MONGO_PASSWORD admin
ENV MONGO_HOST localhost
ENV MONGO_PORT 27017
RUN npm install
CMD npm start
