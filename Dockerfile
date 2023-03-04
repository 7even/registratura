# image that builds a jar
FROM clojure:temurin-11-jammy AS builder

WORKDIR /tmp/build

RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
RUN apt-get install -qq nodejs
RUN npm install -g shadow-cljs

COPY package.json package-lock.json ./
RUN npm install > /dev/null 2>&1

COPY deps.edn .
RUN clj -A:clj:cljs -P > /dev/null 2>&1

COPY . .
RUN shadow-cljs release :main > /dev/null 2>&1
RUN clj -T:build uberjar

# image that runs a jar
FROM eclipse-temurin:11

COPY --from=builder /tmp/build/target/registratura.jar .

CMD java -jar registratura.jar
