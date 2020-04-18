#!/bin/bash

mkdir -p mobilityData/US

countries="AF AO AG AR AW AU AT BH BD BB BY BE BZ BJ BO BA BW BR BG BF KH CM CA CV CL CO CR CI HR CZ DK DO EC EG SV EE FJ FI FR GA GE DE GH GR GT GW HT HN HK HU IN ID IQ IE IL IT JM JP JO KZ KE KW KG LA LV LB LY LI LT LU MY ML MT MU MX MD MN MZ MM NA NP NL NZ NI NE NG MK NO OM PK PA PG PY PE PH PL PT PR QA RE RO RW SA SN SG SK SI ZA KR ES LK SE CH TW TJ TZ TH BS TG TT TR UG AE GB US UY VE VN YE ZM ZW"
states="Alabama Alaska Arizona Arkansas California Colorado Connecticut Delaware Florida Georgia Hawaii Idaho Illinois Indiana Iowa Kansas Kentucky Louisiana Maine Maryland Massachusetts Michigan Minnesota Mississippi Missouri Montana Nebraska Nevada New_Hampshire New_Jersey New_Mexico New_York North_Carolina North_Dakota Ohio Oklahoma Oregon Pennsylvania Rhode_Island South_Carolina South_Dakota Tennessee Texas Utah Vermont Virginia Washington West_Virginia Wisconsin Wyoming"

date="2020-04-11"

cd mobilityData
for country in $countries ; do
  echo "${country}"
  curl -s -O https://www.gstatic.com/covid19/mobility/${date}_"${country}"_Mobility_Report_en.pdf
done

cd US
for state in $states ; do
  echo "${state}"
  curl -s -O https://www.gstatic.com/covid19/mobility/${date}_US_"${state}"_Mobility_Report_en.pdf
done
