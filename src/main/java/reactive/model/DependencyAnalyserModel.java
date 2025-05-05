package reactive.model;

import io.reactivex.rxjava3.core.Observable;
import common.report.ClassDepsReport;
import common.report.PackageDepsReport;
import common.report.ProjectDepsReport;

import java.io.File;

/**
 * Interfaccia per l'analizzatore di dipendenze utilizzando programmazione reattiva.
 * Definisce il contratto per l'analisi delle dipendenze di classi, pacchetti e progetti Java.
 */
public interface DependencyAnalyserModel {

    /**
     * Analizza le dipendenze di un singolo file di classe Java.
     *
     * @param classFile Il file sorgente Java da analizzare
     * @return Un Observable che emette un report delle dipendenze della classe
     */
    Observable<ClassDepsReport> analyzeClass(File classFile);

    /**
     * Analizza le dipendenze di tutte le classi in un pacchetto.
     *
     * @param packageFolder La cartella contenente i file sorgente del pacchetto
     * @return Un Observable che emette un report delle dipendenze del pacchetto
     */
    Observable<PackageDepsReport> analyzePackage(File packageFolder);

    /**
     * Analizza le dipendenze di tutti i pacchetti in un progetto.
     *
     * @param projectFolder La cartella radice del progetto
     * @return Un Observable che emette un report delle dipendenze del progetto
     */
    Observable<ProjectDepsReport> analyzeProject(File projectFolder);

    /**
     * Restituisce un Observable di tutti i file Java all'interno di una cartella
     * e nelle sue sottocartelle.
     *
     * @param folder La cartella da cui iniziare la ricerca
     * @return Un Observable che emette tutti i file Java trovati
     */
    Observable<File> findJavaFiles(File folder);

    /**
     * Restituisce un Observable di tutte le cartelle che contengono almeno un file Java
     * all'interno di una cartella di progetto.
     *
     * @param projectFolder La cartella di progetto da cui iniziare la ricerca
     * @return Un Observable che emette tutte le cartelle che rappresentano pacchetti Java
     */
    Observable<File> findPackageFolders(File projectFolder);
}