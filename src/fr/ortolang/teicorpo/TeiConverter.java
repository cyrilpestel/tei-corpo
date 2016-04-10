/**
 * @author Myriam Majdoub
 * TeiConverter: conversion des fichiers teiml en d'autres formats , regroupe les méthodes communes à toutes les conversions possibles.
 */

package fr.ortolang.teicorpo;

import java.io.File;
import java.util.ArrayList;

public abstract class TeiConverter {

	//Représentation du fichier teiml à convertir
	TeiFile tf;
	//Nom du fichier teiml à convertir
	String inputName;
	//Nom du fichier de sortie
	String outputName;

	/**
	 * Conversion du fichier teiml: initialisation des variables d'instances puis création du nouveau fichier.
	 * @param inputName	Nom du fichier à convertir
	 * @param outputName	Nom du fichier de sortie
	 */
	public TeiConverter(String inputName, String outputName){
		this.tf = new TeiFile(new File(inputName));
		this.inputName = inputName;
		this.outputName = outputName;
	}

	//Initialisation du fichier de sortie
	public abstract void outputWriter();

	//Conversion des données
	public abstract void conversion();

	//Finalisation du fichier de sortie
	public abstract void createOutput();

	//Récupération des informations générales sur la transcription
	public TeiFile.TransInfo getTransInfo(){
		return this.tf.transInfo;
	}

	//Récupération de la liste des locuteurs
	public ArrayList<TeiFile.Participant> getParticipants(){
		return tf.transInfo.participants;
	}

	//Récupération de la transcription
	public TeiFile.Trans getTrans(){
		return this.tf.trans;
	}

	//Renvoie la chaîne de caractère passée en argument si elle n'est pas vide, sinon renvoir une chaîne de caractère vide.
	public static String toString(String s){
		if(!Utils.isNotEmptyOrNull(s)){
			s = "";
		}
		return s;
	}

	//Convertit des secondes en millisecondes
	public static int toMilliseconds(float t){
		return (int)(t * 1000);
	}

	//Convertit le format de date YY en YYYY
	public static String convertYear(String year){
		if(Integer.parseInt(year) < 50){
			return "20" + year;
		}
		else{
			return "19" + year;
		}
	}
}