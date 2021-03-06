package org.swrlapi.converters;

import org.swrlapi.core.OWLClassExpressionResolver;
import org.swrlapi.core.OWLNamedObjectResolver;
import org.swrlapi.core.OWLPropertyExpressionResolver;
import org.swrlapi.core.SWRLRuleEngineBridge;
import org.swrlapi.ext.SWRLAPILiteralFactory;

/**
 * This interface represents provides utility methods converters implemented by target rule engines to convert OWL
 * objects, such as named classes and individuals, and OWL axioms from a native rule engine representation.
 */
public abstract class TargetRuleEngineConverterBase implements TargetRuleEngineConverter
{
	private final SWRLRuleEngineBridge bridge;

	public TargetRuleEngineConverterBase(SWRLRuleEngineBridge bridge)
	{
		this.bridge = bridge;
	}

	protected OWLNamedObjectResolver getOWLNamedObjectResolver()
	{
		return getBridge().getOWLNamedObjectResolver();
	}

	protected OWLClassExpressionResolver getOWLClassExpressionResolver()
	{
		return getBridge().getOWLClassExpressionResolver();
	}

	protected OWLPropertyExpressionResolver getOWLPropertyExpressionResolver()
	{
		return getBridge().getOWLPropertyExpressionResolver();
	}

	protected SWRLAPILiteralFactory getSWRLAPILiteralFactory()
	{
		return getBridge().getSWRLAPILiteralFactory();
	}

	protected SWRLRuleEngineBridge getBridge()
	{
		return this.bridge;
	}
}
