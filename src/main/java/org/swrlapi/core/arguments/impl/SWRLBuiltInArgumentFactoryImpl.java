package org.swrlapi.core.arguments.impl;

import java.net.URI;
import java.util.List;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.swrlapi.core.arguments.SQWRLCollectionBuiltInArgument;
import org.swrlapi.core.arguments.SWRLAnnotationPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLBuiltInArgument;
import org.swrlapi.core.arguments.SWRLBuiltInArgumentFactory;
import org.swrlapi.core.arguments.SWRLClassBuiltInArgument;
import org.swrlapi.core.arguments.SWRLDataPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLDatatypeBuiltInArgument;
import org.swrlapi.core.arguments.SWRLIndividualBuiltInArgument;
import org.swrlapi.core.arguments.SWRLLiteralBuiltInArgument;
import org.swrlapi.core.arguments.SWRLMultiArgument;
import org.swrlapi.core.arguments.SWRLObjectPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLVariableBuiltInArgument;
import org.swrlapi.ext.OWLDatatypeFactory;
import org.swrlapi.ext.OWLLiteralFactory;
import org.swrlapi.ext.impl.DefaultOWLDatatypeFactory;
import org.swrlapi.ext.impl.DefaultOWLLiteralFactory;
import org.swrlapi.xsd.XSDDate;
import org.swrlapi.xsd.XSDDateTime;
import org.swrlapi.xsd.XSDDuration;
import org.swrlapi.xsd.XSDTime;

public class SWRLBuiltInArgumentFactoryImpl implements SWRLBuiltInArgumentFactory
{
	private final OWLDatatypeFactory owlDatatypeFactory;
	private final OWLLiteralFactory owlLiteralFactory;

	public SWRLBuiltInArgumentFactoryImpl()
	{
		this.owlDatatypeFactory = new DefaultOWLDatatypeFactory();
		this.owlLiteralFactory = new DefaultOWLLiteralFactory(this.owlDatatypeFactory);
	}

	@Override
	public SWRLVariableBuiltInArgument createUnboundVariableArgument(String variableName)
	{
		SWRLVariableBuiltInArgument argument = new SWRLVariableBuiltInArgumentImpl(variableName);
		argument.setUnbound();
		return argument;
	}

	@Override
	public SWRLVariableBuiltInArgument createVariableArgument(String variableName)
	{
		return new SWRLVariableBuiltInArgumentImpl(variableName);
	}

	@Override
	public SWRLClassBuiltInArgument createClassArgument(IRI classIRI)
	{
		return new SWRLClassBuiltInArgumentImpl(classIRI);
	}

	@Override
	public SWRLClassBuiltInArgument createClassArgument(OWLClass cls)
	{
		return new SWRLClassBuiltInArgumentImpl(cls);
	}

	@Override
	public SWRLObjectPropertyBuiltInArgument createObjectPropertyArgument(IRI propertyIRI)
	{
		return new SWRLObjectPropertyBuiltInArgumentImpl(propertyIRI);
	}

	@Override
	public SWRLObjectPropertyBuiltInArgument createObjectPropertyArgument(OWLObjectProperty property)
	{
		return new SWRLObjectPropertyBuiltInArgumentImpl(property);
	}

	@Override
	public SWRLDataPropertyBuiltInArgument createDataPropertyArgument(IRI propertyIRI)
	{
		return new SWRLDataPropertyBuiltInArgumentImpl(propertyIRI);
	}

	@Override
	public SWRLDataPropertyBuiltInArgument createDataPropertyArgument(OWLDataProperty property)
	{
		return new SWRLDataPropertyBuiltInArgumentImpl(property);
	}

	@Override
	public SWRLAnnotationPropertyBuiltInArgument createAnnotationPropertyArgument(IRI propertyIRI)
	{
		return new SWRLAnnotationPropertyBuiltInArgumentImpl(propertyIRI);
	}

	@Override
	public SWRLAnnotationPropertyBuiltInArgument createAnnotationPropertyArgument(OWLAnnotationProperty property)
	{
		return new SWRLAnnotationPropertyBuiltInArgumentImpl(property);
	}

	@Override
	public SWRLDatatypeBuiltInArgument createDatatypeArgument(IRI iri)
	{
		return new SWRLDatatypeBuiltInArgumentImpl(iri);
	}

	@Override
	public SWRLDatatypeBuiltInArgument createDatatypeArgument(OWLDatatype datatype)
	{
		return new SWRLDatatypeBuiltInArgumentImpl(datatype);
	}

	@Override
	public SWRLIndividualBuiltInArgument createIndividualArgument(IRI individualIRI)
	{
		return new SWRLIndividualBuiltInArgumentImpl(individualIRI);
	}

	@Override
	public SWRLIndividualBuiltInArgument createIndividualArgument(OWLIndividual individual)
	{
		if (individual.isNamed()) {
			OWLNamedIndividual namedIndividual = individual.asOWLNamedIndividual();
			return new SWRLIndividualBuiltInArgumentImpl(namedIndividual.getIRI());
		} else
			throw new RuntimeException("OWL anonymous individual built-in arguments not supported by Portability API");
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(OWLLiteral literal)
	{
		return new SWRLLiteralBuiltInArgumentImpl(literal);
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(String s)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(s));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(boolean b)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(b));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(int i)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(i));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(long l)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(l));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(float f)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(f));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(double d)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(d));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(byte b)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(b));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(URI uri)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(uri));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(XSDDate date)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(date));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(XSDTime time)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(time));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(XSDDateTime datetime)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(datetime));
	}

	@Override
	public SWRLLiteralBuiltInArgument createLiteralArgument(XSDDuration duration)
	{
		return new SWRLLiteralBuiltInArgumentImpl(getOWLLiteralFactory().getOWLLiteral(duration));
	}

	@Override
	public SWRLMultiArgument createMultiArgument()
	{
		return new SWRLMultiArgumentImpl();
	}

	@Override
	public SWRLMultiArgument createMultiArgument(List<SWRLBuiltInArgument> arguments)
	{
		return new SWRLMultiArgumentImpl(arguments);
	}

	@Override
	public SQWRLCollectionBuiltInArgument createSQWRLCollectionArgument(String queryName, String collectionName,
			String collectionGroupID)
	{
		return new SQWRLCollectionBuiltInArgumentImpl(queryName, collectionName, collectionGroupID);
	}

	private OWLLiteralFactory getOWLLiteralFactory()
	{
		return this.owlLiteralFactory;
	}
}