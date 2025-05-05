package asynchronous.analyser;

import io.vertx.core.Vertx;

public class SimulationAnalyser {
    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new DependencyAnalyserVerticle())
                .onComplete(r -> vertx.close());
    }
}
