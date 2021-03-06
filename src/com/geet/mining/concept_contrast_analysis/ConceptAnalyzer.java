package com.geet.mining.concept_contrast_analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.geet.mining.model.Event;
import com.geet.mining.model.Issue;
import com.geet.mining.model.Node;
import com.geet.mining.model.TransactionModule;

/**
 * 
 * @author chandradasdipok 11-Nov-2017 This class analyzes a single issue and
 *         helps to formulate the Formal Concept Analysis (FCA)
 *
 */
public class ConceptAnalyzer {

	// the anylzed issue
	Issue issue = null;

	public ConceptAnalyzer(Issue issue) {
		this.issue = issue;
		int i=0;
		for (Event event : issue.getEvents()) {
			event.setValue(i);
			for (String moduleKey : issue.getTransactionModules().keySet()) {
				for (Event evt : issue.getTransactionModules().get(moduleKey).eventSet) {
					if (evt.getEventString().equals(event.getEventString())) {
						evt.setValue(i);
					}
				}
			}
			i++;
		}
		/*for (Event event : issue.getEvents()) {
			System.out.println(event.getEventString()+","+event.getValue());
		}
		System.out.println("Transactions");
		for (String moduleKey : issue.getTransactionModules().keySet()) {
			System.out.println(moduleKey);
			for (Event evt : issue.getTransactionModules().get(moduleKey).eventSet) {
				System.out.println(evt.getEventString()+","+evt.getValue());
			}
		}*/

	}

	// Generate all the nodes of fca graph of an issue
	// using next closure algorithms
	// The algorithm is proposed by Ganter et. al at 1992
	public Set<Node> generateNodesOfGraph() {
		Set<Node> nodes = new HashSet<Node>();
		List<Event> attributes = new ArrayList<>(Event.getClonedEvents(issue.getEvents()));
		/*System.out.println("Eventsssssssss");
		for (Event event : attributes) {
			System.out.println(event+","+event.getValue());
		}*/
		Set<Event> closedSet = getFirstClosure();
		int i = 0;
		while (i < 50) {
			System.out.println("No. " + i + ": Closed Sets " + closedSet);
			Node node = generateNodeFromAClosedSet(Event.getClonedEvents(closedSet));
			System.out.println(node);
			nodes.add(node);
			if (closedSet.equals(issue.getEvents())) {
				break;
			}
			closedSet = Event.getClonedEvents(getNextClosedSet(Event.getClonedEvents(closedSet), attributes));
			i++;
		}
		return nodes;
	}
	
	// this is the first closure
	// by default, the first closure is empty set
	private Set<Event> getFirstClosure() {
		return new HashSet<Event>();
	}

	// next closure algorithms
		// The algorithm is proposed by Ganter et. al at 1992
		private Set<Event> getNextClosedSet(Set<Event> closedSet, List<Event> attributes) {
			for (int i = attributes.size() - 1; i >= 0; i--) {
				Set<Event> nextClosedSet = new HashSet<Event>();
				Event m = attributes.get(i);
				// System.out.println("Element "+m);
				if (closedSet.contains(m)) {
					closedSet.remove(m);
					// System.out.println("Closed Set after remove "+closedSet);
				} else {
					nextClosedSet.addAll(closedSet);
					nextClosedSet.add(m);
					// System.out.println("Next Closed Set "+nextClosedSet);
					// System.out.println("Total Events "+issue.getEvents());
					// System.out.println("Closures:
					// "+closureOfEvents(nextClosedSet, CONTEXT_TABLE));
					 nextClosedSet = closureOfEvents(nextClosedSet);
					// System.out.println("Closures of Next Closed Set
					// "+nextClosedSet);
					if (!hasLessThanElementM(Event.getClonedEvents(nextClosedSet), closedSet, m)) {
						return nextClosedSet;
					}
				}
			}
			return new HashSet<Event>();
		}
	
	// Here events are attributes of FCA
	// It takes the attribute set
	// returns the closures of given attributes
	private Set<Event> closureOfEvents(Set<Event> events) {
		Set<Event> closure = new HashSet<Event>();
		Set<String> transactionsID = new HashSet<String>();

		// collects the transactions which has common attributes i.e., events
		for (String moduleKey : issue.getTransactionModules().keySet()) {
			if (issue.getTransactionModules().get(moduleKey).eventSet.containsAll(events)) {
				transactionsID.add(moduleKey);
				// System.out.println("Module Key"+moduleKey);
			}
		}

		// if the object set of common attributes i.e., events is empty then
		// return all the attributes
		if (transactionsID.size() == 0) {
			return Event.getClonedEvents(issue.getEvents());
		}
		// other wise take the intersection of all the events of objects
		else {
			for (String transactionKey : transactionsID) {
				if (closure.size() == 0) {
					// store first module's events as closure
					closure = Event.getClonedEvents(issue.getTransactionModules().get(transactionKey).eventSet);
				} else {
					// intersection of module's events given transactions with
					// closure
					closure.retainAll(
							Event.getClonedEvents(issue.getTransactionModules().get(transactionKey).eventSet));
				}
			}
		}
		return closure;
	}

	

	// detect whether there is difference between closed set and
	// next closed set less than m
	private boolean hasLessThanElementM(Set<Event> nextClosedSet, Set<Event> closedSet, Event eventM) {
		// System.out.println("Element Check "+eventM);
		// System.out.println(nextClosedSet);
		// System.out.println(closedSet);
		Set<Event> diff = nextClosedSet;
		diff.removeAll(closedSet);
		// System.out.println(diff);
		// if has elements less than eventM
		// return true
		// else return false
		for (Event event : diff) {
			//if (eventM.getEventString().compareTo(event.getEventString()) > 0) {
			//System.out.println(eventM.getValue()+","+event.getValue());
			if(eventM.getValue() > event.getValue()){
				// System.out.println("Smallest New Element "+ event);
				return true;
			}
		}
		return false;
	}

	// generate a node of FCA graph from a closed set
	private Node generateNodeFromAClosedSet(Set<Event> closedSet) {
		Node generatedNode = new Node();
		generatedNode.setClosedSet(closedSet);
		for (String moduleKey : issue.getTransactionModules().keySet()) {
			TransactionModule transactionModule = issue.getTransactionModules().get(moduleKey);
			// System.out.println(transactionModule.toString());
			if (transactionModule.eventSet.containsAll(closedSet)) {
				generatedNode.setSucceed(generatedNode.getSucceed() + transactionModule.succeed);
				generatedNode.setFail(generatedNode.getFail() + transactionModule.fail);
			}
		}
		// System.out.println(generatedNode);
		return generatedNode;
	}

}
