package asynchronous;

import asynchronous.analyser.DependencyAnalyserVerticle;
import io.vertx.core.Vertx;


/* Implemented by:
    Giacomo Foschi
    Matricola: 0001179137
    Email: giacomo.foschi3@studio.unibo.it

    Giovanni Pisoni
    Matricola: 0001189814
    Email: giovanni.pisoni@studio.unibo.it

    Giovanni Rinchiuso
    Matricola: 0001195145
     Email: giovanni.rinchiuso@studio.unibo.it

    Gioele Santi
    Matricola: 0001189403
    Email: gioele.santi2@studio.unibo.it
*/
public class SimulationAnalyser {
    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new DependencyAnalyserVerticle())
                .onComplete(r -> vertx.close());
    }
}
