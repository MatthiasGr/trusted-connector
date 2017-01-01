import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule }   from '@angular/forms';

import { IdsComponent } from './ids.component';
import { routing } from './ids.routing';


@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    routing
  ],
  declarations: [
    IdsComponent,
  ]
})
export default class CamelRouteModule {}
