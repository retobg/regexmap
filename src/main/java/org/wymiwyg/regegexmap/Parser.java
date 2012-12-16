package org.wymiwyg.regegexmap;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Parser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//State startSparse("hello");

	}

	public static State parse(String string) {
		try {
			return parse(new PushbackReader(new StringReader(string), 1));
		} catch (IOException e) {
			throw new RuntimeException("IO reading from string", e);
		}
	}

	private static State parse(PushbackReader in) throws IOException {
		State startState = new State();
		Set<State> exitStates = parseInto(startState, startState, in);
		for (State state : exitStates) {
			state.markAsEndState();
		}
		return startState;
	}

	/**
	 * @return the exit states
	 */
	private static Set<State> parseInto(State startState, State state, PushbackReader in) throws IOException {
		
		int currentChar = in.read();
		switch (currentChar) {
			case -1 : return Collections.singleton(state);
			case '|':
				Set<State> exitStates = new HashSet<State>();
				exitStates.add(state); 
				exitStates.addAll(parseInto(startState, startState, in));
				return exitStates;
			default:
				int nextChar = in.read();
				State targetState;
				switch (nextChar) {
					case -1: targetState = new State(); break;
					case '*': targetState = state; break;
					default: in.unread(nextChar); targetState = new State();
				}
				if (currentChar == '.') {
					state.transitions.clear();
					state.addTransition(new AcceptAllExceptSpecified(targetState));
				} else {
					final Set<State> statesOfWhichTransitionsMustBeaddedToTarget 
						= removeCharFromTransitions(state.transitions, (char) currentChar);
					state.addTransition(new AcceptSingle(targetState, (char) currentChar));
					for (State stateToCopyIntoTarget : statesOfWhichTransitionsMustBeaddedToTarget) {
						targetState.transitions.addAll(stateToCopyIntoTarget.transitions);
					}
				}
				return parseInto(startState, targetState, in);
		}
	}

	/**
	 * excludes a char from all transitions in a set and returns the targets of 
	 * the affected transitions, transition that will no longer accept anything are removed from the set
	 */
	private static Set<State> removeCharFromTransitions(Set<Transition> transitions,
			char ch) {
		final Set<State> result = new HashSet<State>();
		Iterator<Transition> iter = transitions.iterator();
		while (iter.hasNext()) {
			Transition transition = iter.next();
			if (transition.accepts(ch)) {
				if (transition instanceof AcceptSingle) {
					iter.remove();
				} else {
					transition.exclude(ch);
				}
				result.add(transition.getTarget());
			}
		}
		return result;
	}

}
