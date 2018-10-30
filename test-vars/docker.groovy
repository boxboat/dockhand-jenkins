class DockerImage {

    def inside(Closure next){
        next()
    }

    def push(String tag){
    }

}

def call() {
    println "docker"
}

def withRegistry(String uri, String credentials, Closure next){
    next()
}

def withServer(String uri, String credentials, Closure next){
    next()
}

def build(String tag){
    return new DockerImage()
}

def image(String tag){
    return new DockerImage()
}
